package noodle.oauth.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import noodle.oauth.core.domain.User
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    fun getUser(): Unit =
        runBlocking {
            val user = repository.getUser(id, loginId)
            assertEquals(id, user.id)
            assertEquals(loginId, user.loginId)
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(id, loginId)
        }
}
