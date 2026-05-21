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

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(address)
        }
}
