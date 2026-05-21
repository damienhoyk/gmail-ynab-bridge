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
import java.util.UUID

@TestMethodOrder(OrderAnnotation::class)
@TestInstance(PER_CLASS)
class DynamoDbBridgeRepositoryTests {
    private val repository = DynamoDbBridgeRepository(environment = "test")
    private val source = "test-${UUID.randomUUID()}@gmail.com"
    private val destination = UUID.randomUUID().toString()

    @Order(1)
    @Test
    fun put(): Unit =
        runBlocking {
            repository.put(source, destination)
        }

    @Test
    fun get(): Unit =
        runBlocking {
            val result = repository.get(source, destination)
            val item = result.item()
            assertEquals(source, item["source"]?.s())
        }

    @Test
    fun getBridge(): Unit =
        runBlocking {
            val result = repository.getBridge(source, destination)
            assertEquals(source, result.source)
            assertEquals(destination, result.destination)
            assertEquals(emptyMap<String, String>(), result.accounts)
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(source, destination)
        }
}
