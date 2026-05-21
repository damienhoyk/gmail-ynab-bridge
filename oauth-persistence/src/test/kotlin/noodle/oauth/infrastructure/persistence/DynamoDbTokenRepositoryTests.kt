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
    private val type = listOf("state", "access", "refresh").random()
    private val value = UUID.randomUUID().toString()

    @Order(1)
    @Test
    fun put(): Unit =
        runBlocking {
            repository.put(id, type) { put("value", fromS(value)) }
        }

    @Order(2)
    @Test
    fun getToken(): Unit =
        runBlocking {
            val token = repository.getToken(id, type)
            assertEquals(id, token.id)
            assertEquals(type, token.type)
            assertEquals(value, token.value)
        }

    @Order(3)
    @Test
    fun updateTokenValue(): Unit =
        runBlocking {
            val newValue = UUID.randomUUID().toString()
            val token = repository.updateTokenValue(id, type, newValue)
            assertEquals(newValue, token.value)
        }

    @Order(4)
    @Test
    fun get(): Unit =
        runBlocking {
            val result = repository.get(id, type)
            val item = result.item()
            assertEquals(id, item["id"]?.s())
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(id, type)
        }
}
