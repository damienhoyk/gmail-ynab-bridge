package noodle.ynabsync.infrastructure.persistence

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
class DynamoDbAccessTokenRepositoryTests {
    private val repository = DynamoDbAccessTokenRepository(environment = "test")
    private val id = UUID.randomUUID().toString()
    private val accessToken = UUID.randomUUID().toString()

    @Order(1)
    @Test
    fun put(): Unit =
        runBlocking {
            repository.put(id, "access") { put("value", fromS(accessToken)) }
        }

    @Test
    fun getAccessToken(): Unit =
        runBlocking {
            assertEquals(accessToken, repository.getAccessToken(id))
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(id, "access")
        }
}
