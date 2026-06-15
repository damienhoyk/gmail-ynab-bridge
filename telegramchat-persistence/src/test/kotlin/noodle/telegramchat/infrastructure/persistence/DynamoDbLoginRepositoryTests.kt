package noodle.telegramchat.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import noodle.telegramchat.core.domain.Login
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
class DynamoDbLoginRepositoryTests {
    private val repository = DynamoDbLoginRepository(environment = "test")
    private val id = UUID.randomUUID().toString()
    private val userId = "test-${UUID.randomUUID()}@gmail.com"

    @Order(1)
    @Test
    fun putLogin(): Unit =
        runBlocking {
            repository.putLogin(Login(id, userId))
        }

    @Test
    fun getLogin(): Unit =
        runBlocking {
            val login = repository.getLogin(id)
            assertEquals(id, login?.id)
            assertEquals(userId, login?.userId)
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(id)
        }
}
