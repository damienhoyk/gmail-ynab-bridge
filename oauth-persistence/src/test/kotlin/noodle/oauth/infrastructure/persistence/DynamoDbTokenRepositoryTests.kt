package noodle.oauth.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import java.util.UUID

@TestMethodOrder(OrderAnnotation::class)
@TestInstance(PER_CLASS)
class DynamoDbTokenRepositoryTests {
    private val repository = DynamoDbTokenRepository(environment = "test")
    private val id = UUID.randomUUID().toString()
    private val accessToken = UUID.randomUUID().toString()
    private val refreshToken = UUID.randomUUID().toString()

    @Order(1)
    @Test
    fun put(): Unit =
        runBlocking {
            repository.put(id, "access") { put("value", fromS(accessToken)) }
            repository.put(id, "refresh") { put("value", fromS(refreshToken)) }
        }

    @Order(2)
    @Test
    fun getAccessToken(): Unit =
        runBlocking {
            val value = repository.getAccessToken(id)
            assertEquals(accessToken, value)
        }

    @Order(3)
    @Test
    fun getRefreshToken(): Unit =
        runBlocking {
            val value = repository.getRefreshToken(id)
            assertEquals(refreshToken, value)
        }

    @Order(4)
    @Test
    fun updateTokenValue(): Unit =
        runBlocking {
            val newValue = UUID.randomUUID().toString()
            val token = repository.updateTokenValue(id, "access", newValue)
            assertEquals(newValue, token.value)
        }

    @Order(5)
    @Test
    fun get(): Unit =
        runBlocking {
            val result = repository.get(id, "access")
            val item = result.item()
            assertEquals(id, item["id"]?.s())
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(id, "access")
            repository.delete(id, "refresh")
        }
}
