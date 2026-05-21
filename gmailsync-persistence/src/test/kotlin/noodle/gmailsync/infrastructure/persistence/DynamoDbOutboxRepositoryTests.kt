package noodle.gmailsync.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import noodle.gmailsync.core.domain.Outbox
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID

@TestMethodOrder(OrderAnnotation::class)
@TestInstance(PER_CLASS)
class DynamoDbOutboxRepositoryTests {
    private val repository = DynamoDbOutboxRepository(environment = "test")
    private val destination = "${UUID.randomUUID()}@app.ynab.com"
    private val address = "test-${UUID.randomUUID()}@gmail.com"
    private val mailId = UUID.randomUUID().toString()
    private val source = "$mailId:$address"

    @Order(1)
    @Test
    fun putOutbox(): Unit =
        runBlocking {
            repository.putOutbox(Outbox(destination, source))
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(destination, source)
        }
}
