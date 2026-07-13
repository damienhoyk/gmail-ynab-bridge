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
    fun updateMailbox(): Unit =
        runBlocking {
            repository.updateMailbox(Mailbox(address, state))
        }

    @Test
    fun getMailbox(): Unit =
        runBlocking {
            val result = repository.getMailbox(address)
            assertEquals(address, result.address)
            assertEquals(state, result.state)
        }

    @Test
    fun `updateMailbox preserves expiration attribute`(): Unit =
        runBlocking {
            val expirationAddress = "test-preserve-${UUID.randomUUID()}@gmail.com"
            val initialState = (100000..199999).random().toLong()
            val seededExpiration = "12345"
            val newState = (200000..299999).random().toLong()

            // Seed row with expiration using raw write
            repository.update(expirationAddress) {
                put("expiration", fromN(seededExpiration))
            }

            // Call updateMailbox to update state via port
            repository.updateMailbox(Mailbox(expirationAddress, newState))

            // Read raw row and assert both state is updated AND expiration is preserved
            val item = repository.get(expirationAddress).item()
            assertEquals(newState.toString(), item["state"]?.n())
            assertEquals(seededExpiration, item["expiration"]?.n())

            // Cleanup
            repository.delete(expirationAddress)
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(address)
        }
}
