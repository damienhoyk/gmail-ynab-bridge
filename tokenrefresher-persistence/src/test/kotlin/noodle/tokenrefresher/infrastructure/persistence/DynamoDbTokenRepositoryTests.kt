package noodle.tokenrefresher.infrastructure.persistence

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import java.time.Instant
import java.util.UUID

@TestMethodOrder(OrderAnnotation::class)
@TestInstance(PER_CLASS)
class DynamoDbTokenRepositoryTests {
    private val repository = DynamoDbTokenRepository(environment = "test")
    private val seededItems = mutableListOf<Pair<String, String>>()

    @Order(1)
    @Test
    fun `access row with expired expiresAt and refresh row is found by findRefreshable`(): Unit =
        runBlocking {
            val id = UUID.randomUUID().toString()
            val accessToken = "access-token-${UUID.randomUUID()}"
            val refreshToken = "refresh-token-${UUID.randomUUID()}"
            val expiredTime = Instant.now().epochSecond - 100 // 100 seconds in the past

            // Seed access token with expired expiresAt
            repository.put(id, "access") {
                put("value", fromS(accessToken))
                put("expiresAt", fromN("$expiredTime"))
            }
            seededItems.add(id to "access")

            // Seed refresh token
            repository.put(id, "refresh") {
                put("value", fromS(refreshToken))
            }
            seededItems.add(id to "refresh")

            // Find refreshable tokens
            val refreshable = repository.findRefreshable().toList().flatMap { it }

            // Assert the token was found
            val found = refreshable.find { it.id == id }
            assertEquals(id, found?.id, "Should find token with expired expiresAt")
            assertEquals(refreshToken, found?.refreshToken, "Should have correct refresh token value")
        }

    @Order(2)
    @Test
    fun `access row without expiresAt attribute and refresh row is found by findRefreshable`(): Unit =
        runBlocking {
            val id = UUID.randomUUID().toString()
            val accessToken = "access-token-${UUID.randomUUID()}"
            val refreshToken = "refresh-token-${UUID.randomUUID()}"

            // Seed access token without expiresAt
            repository.put(id, "access") {
                put("value", fromS(accessToken))
            }
            seededItems.add(id to "access")

            // Seed refresh token
            repository.put(id, "refresh") {
                put("value", fromS(refreshToken))
            }
            seededItems.add(id to "refresh")

            // Find refreshable tokens
            val refreshable = repository.findRefreshable().toList().flatMap { it }

            // Assert the token was found
            val found = refreshable.find { it.id == id }
            assertEquals(id, found?.id, "Should find token without expiresAt attribute")
            assertEquals(refreshToken, found?.refreshToken, "Should have correct refresh token value")
        }

    @Order(3)
    @Test
    fun `access row with future expiresAt is not found by findRefreshable`(): Unit =
        runBlocking {
            val id = UUID.randomUUID().toString()
            val accessToken = "access-token-${UUID.randomUUID()}"
            val refreshToken = "refresh-token-${UUID.randomUUID()}"
            val futureTime = Instant.now().epochSecond + 7200 // 2 hours in the future

            // Seed access token with future expiresAt
            repository.put(id, "access") {
                put("value", fromS(accessToken))
                put("expiresAt", fromN("$futureTime"))
            }
            seededItems.add(id to "access")

            // Seed refresh token
            repository.put(id, "refresh") {
                put("value", fromS(refreshToken))
            }
            seededItems.add(id to "refresh")

            // Find refreshable tokens
            val refreshable = repository.findRefreshable().toList().flatMap { it }

            // Assert the token was not found
            val found = refreshable.find { it.id == id }
            assertEquals(null, found, "Should not find token with future expiresAt")
        }

    @Order(4)
    @Test
    fun `access row with expired expiresAt but missing refresh row is excluded`(): Unit =
        runBlocking {
            val id = UUID.randomUUID().toString()
            val accessToken = "access-token-${UUID.randomUUID()}"
            val expiredTime = Instant.now().epochSecond - 100

            // Seed access token with expired expiresAt (no refresh token)
            repository.put(id, "access") {
                put("value", fromS(accessToken))
                put("expiresAt", fromN("$expiredTime"))
            }
            seededItems.add(id to "access")

            // Find refreshable tokens
            val refreshable = repository.findRefreshable().toList().flatMap { it }

            // Assert the token was not found
            val found = refreshable.find { it.id == id }
            assertEquals(null, found, "Should exclude token with missing refresh row")
        }

    @Order(5)
    @Test
    fun `updateAccess writes value and expiresAt`(): Unit =
        runBlocking {
            val id = UUID.randomUUID().toString()
            val newAccessToken = "new-access-token-${UUID.randomUUID()}"
            val expiresIn = 3600L

            // First put a stub access token
            repository.put(id, "access") {
                put("value", fromS("old-token"))
            }
            seededItems.add(id to "access")

            val beforeUpdate = Instant.now().epochSecond

            // Update access token with expiresIn
            repository.updateAccess(id, newAccessToken, expiresIn)

            val afterUpdate = Instant.now().epochSecond

            // Retrieve the updated access token
            val response = repository.get(id, "access")
            val item = response.item()

            assertEquals(newAccessToken, item?.get("value")?.s(), "Should have new access token value")

            val expiresAt = item?.get("expiresAt")?.n()?.toLong()
            assertNotNull(expiresAt, "Should have expiresAt attribute")

            val expectedExpiration = beforeUpdate + expiresIn
            val tolerance = 5L // Allow 5 seconds tolerance for test execution time
            assertTrue(
                expiresAt!! >= expectedExpiration - tolerance && expiresAt <= afterUpdate + expiresIn + tolerance,
                "expiresAt should be approximately now + expiresIn (actual: $expiresAt, expected range: ${expectedExpiration - tolerance} to ${afterUpdate + expiresIn + tolerance})",
            )
        }

    @Order(6)
    @Test
    fun `updateAccess does not modify refresh token`(): Unit =
        runBlocking {
            val id = UUID.randomUUID().toString()
            val accessToken = "access-token-${UUID.randomUUID()}"
            val refreshToken = "refresh-token-${UUID.randomUUID()}"
            val expiresIn = 3600L

            // Seed both tokens
            repository.put(id, "access") {
                put("value", fromS("old-access"))
            }
            seededItems.add(id to "access")

            repository.put(id, "refresh") {
                put("value", fromS(refreshToken))
            }
            seededItems.add(id to "refresh")

            // Update access token
            repository.updateAccess(id, accessToken, expiresIn)

            // Verify refresh token is unchanged
            val refreshResponse = repository.get(id, "refresh")
            val refreshItem = refreshResponse.item()

            assertEquals(refreshToken, refreshItem?.get("value")?.s(), "Refresh token should be unchanged")
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            seededItems.forEach { (id, type) ->
                repository.delete(id, type)
            }
        }

    private fun assertNotNull(
        value: Any?,
        message: String,
    ) {
        assertTrue(value != null, message)
    }
}
