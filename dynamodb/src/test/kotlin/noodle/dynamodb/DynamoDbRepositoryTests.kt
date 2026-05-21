package noodle.dynamodb

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
class DynamoDbRepositoryTests {
    private val repository = TestDynamoDbRepository(environment = "test")
    private val id = UUID.randomUUID().toString()
    private val value = UUID.randomUUID().toString()

    @Order(1)
    @Test
    fun put(): Unit =
        runBlocking {
            repository.put(id) { put("value", fromS(value)) }
        }

    @Order(2)
    @Test
    fun get(): Unit =
        runBlocking {
            val item = repository.get(id).item()
            assertEquals(id, item["partition"]?.s())
            assertEquals(value, item["value"]?.s())
        }

    @Order(3)
    @Test
    fun update(): Unit =
        runBlocking {
            val newValue = UUID.randomUUID().toString()
            val item = repository.update(id) { put("value", fromS(newValue)) }.attributes()
            assertEquals(id, item["partition"]?.s())
            assertEquals(newValue, item["value"]?.s())
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(id)
        }
}
