package noodle.telegramchat.infrastructure.persistence

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
    private val token = UUID.randomUUID().toString()

    @Order(1)
    @Test
    fun seedAccessToken(): Unit =
        runBlocking {
            repository.put(id, "access") { put("value", fromS(token)) }
        }

    @Test
    fun getAccessToken(): Unit =
        runBlocking {
            val value = repository.getAccessToken(id)
            assertEquals(token, value)
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(id, "access")
        }
}
