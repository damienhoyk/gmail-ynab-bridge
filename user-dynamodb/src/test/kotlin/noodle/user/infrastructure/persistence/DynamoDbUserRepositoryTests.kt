package noodle.user.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynamoDbUserRepositoryTests {
    val repository = DynamoDbUserRepository(environment = "test")
    val id = UUID.randomUUID().toString()
    val loginId = "test-${UUID.randomUUID()}@gmail.com"

    @Order(1)
    @Test
    fun put(): Unit =
        runBlocking {
            repository.put(id, loginId)
        }

    @Test
    fun get(): Unit =
        runBlocking {
            val result = repository.get(id, loginId)
            val item = result.item()

            Assertions.assertEquals(id, item["id"]?.s())
            Assertions.assertEquals(loginId, item["loginId"]?.s())
        }

    @Test
    fun query(): Unit =
        runBlocking {
            val results = repository.query(id)
            Assertions.assertEquals(1, results.count())
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(id, loginId)
        }
}
