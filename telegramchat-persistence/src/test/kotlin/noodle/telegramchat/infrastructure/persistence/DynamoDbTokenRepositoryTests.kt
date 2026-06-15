package noodle.telegramchat.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import noodle.telegramchat.core.domain.StateToken
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

@TestMethodOrder(OrderAnnotation::class)
@TestInstance(PER_CLASS)
class DynamoDbTokenRepositoryTests {
    private val repository = DynamoDbTokenRepository(environment = "test")
    private val id = UUID.randomUUID().toString()
    private val userId = "test-${UUID.randomUUID()}@gmail.com"
    private val duration = 10.minutes

    @Order(1)
    @Test
    fun putToken(): Unit =
        runBlocking {
            repository.putToken(StateToken(id, userId, duration))
        }

    @Test
    fun getToken(): Unit =
        runBlocking {
            val result = repository.get(id, "state")
            val item = result.item()
            assertEquals(userId, item["value"]?.s())
            assertNotNull(item["expiresAt"])
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(id, "state")
        }
}
