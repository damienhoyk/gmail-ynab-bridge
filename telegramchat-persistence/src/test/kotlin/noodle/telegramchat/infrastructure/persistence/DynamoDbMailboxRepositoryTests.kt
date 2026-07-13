package noodle.telegramchat.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import noodle.telegramchat.core.domain.Mailbox
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
    private val expiration = (100000..199999).random().toLong()

    @Order(1)
    @Test
    fun updateMailbox(): Unit =
        runBlocking {
            repository.updateMailbox(Mailbox(address, state, expiration))
        }

    @Test
    fun getMailbox(): Unit =
        runBlocking {
            val result = repository.get(address)
            val item = result.item()
            val stateValue = item["state"]?.n()?.toLong()
            assertEquals(state, stateValue)
            val expirationValue = item["expiration"]?.n()?.toLong()
            assertEquals(expiration, expirationValue)
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(address)
        }
}
