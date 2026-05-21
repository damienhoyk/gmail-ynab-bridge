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
import kotlin.time.Clock.System.now
import kotlin.time.Duration.Companion.hours

@TestMethodOrder(OrderAnnotation::class)
@TestInstance(PER_CLASS)
class DynamoDbOutboxRepositoryTests {
    private val repository = DynamoDbOutboxRepository(environment = "test")
    private val destination = "${UUID.randomUUID()}@app.ynab.com"
    private val mailId = UUID.randomUUID().toString()
    private val address = "test-${UUID.randomUUID()}@gmail.com"
    private val source = "$mailId:$address"

    @Order(1)
    @Test
    fun updateTtl(): Unit =
        runBlocking {
            val ttl = repository.updateTtl(destination, source, 1.hours)
            assertEquals((now() + 1.hours).epochSeconds, ttl)
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(destination, source)
        }
}
