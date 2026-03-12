package noodle.bridge.infrastructure.out

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
class DynamoDbMatcherRepositoryTests {
    private val repository = DynamoDbMatcherRepository(environment = "test")
    private val source = "test-${UUID.randomUUID()}@gmail.com"
    private val mode = "card"

    @Order(1)
    @Test
    fun put(): Unit =
        runBlocking {
            repository.put(source, mode) {
                put("pattern", fromS("some-pattern"))
            }
        }

    @Test
    fun get(): Unit =
        runBlocking {
            val result = repository.get(source, mode)
            val item = result.item()
            assertEquals(source, item["source"]?.s())
        }

    @Test
    fun query(): Unit =
        runBlocking {
            val results = repository.query(source)
            assertEquals(1, results.count())
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(source, mode)
        }
}
