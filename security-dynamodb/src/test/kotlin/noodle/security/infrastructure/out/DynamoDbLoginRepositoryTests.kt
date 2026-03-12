package noodle.security.infrastructure.out

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
class DynamoDbLoginRepositoryTests {
    private val repository = DynamoDbLoginRepository(environment = "test")
    private val id = UUID.randomUUID().toString()
    private val userId = "test-${UUID.randomUUID()}@gmail.com"

    @Order(1)
    @Test
    fun put(): Unit =
        runBlocking {
            repository.put(id) { put("userId", fromS(userId)) }
        }

    @Test
    fun get(): Unit =
        runBlocking {
            val result = repository.get(id)
            val item = result.item()
            assertEquals(id, item["id"]?.s())
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(id)
        }
}
