package noodle.gmailsync.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import noodle.gmailsync.core.domain.Mailbox
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import java.util.UUID

@TestMethodOrder(OrderAnnotation::class)
@TestInstance(PER_CLASS)
class DynamoDbMailboxRepositoryTests {
    private val repository = DynamoDbMailboxRepository(environment = "test")
    private val address = "test-${UUID.randomUUID()}@gmail.com"
    private val state = (100000..199999).random().toLong()

    @Order(1)
    @Test
    fun putMailbox(): Unit =
        runBlocking {
            repository.putMailbox(Mailbox(address, state))
        }

    @Test
    fun getMailbox(): Unit =
        runBlocking {
            val result = repository.getMailbox(address)
            assertEquals(address, result.address)
            assertEquals(state, result.state)
        }

    @Test
    fun update(): Unit =
        runBlocking {
            val item = repository.update(address) { put("state", fromN("$state")) }.attributes()
            assertEquals(address, item["address"]?.s())
            assertEquals(state, item["state"]?.n()?.toLong())
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(address)
        }
}
