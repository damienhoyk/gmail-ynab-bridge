package noodle.telegramchat.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import noodle.telegramchat.core.domain.User
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
class DynamoDbUserRepositoryTests {
    private val repository = DynamoDbUserRepository(environment = "test")
    private val id = UUID.randomUUID().toString()
    private val loginId = "test-${UUID.randomUUID()}@gmail.com"

    @Order(1)
    @Test
    fun putUser(): Unit =
        runBlocking {
            repository.putUser(User(id, loginId))
        }

    @Test
    fun queryUser(): Unit =
        runBlocking {
            val users = repository.queryUser(id)
            val user = users.find { it.id == id }
            assertEquals(id, user?.id)
            assertEquals(loginId, user?.loginId)
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(id, loginId)
        }
}
