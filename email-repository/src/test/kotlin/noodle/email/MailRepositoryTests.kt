package noodle.email

import kotlinx.coroutines.runBlocking
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
class MailRepositoryTests {

    val repository = MailRepository(environment = "test")
    val address = "test-${UUID.randomUUID()}@gmail.com"
    val mailId = UUID.randomUUID().toString()

    @Order(1)
    @Test
    fun put(): Unit = runBlocking {
        repository.put(address, mailId)
    }

    @Test
    fun get(): Unit = runBlocking {
        val result = repository.get(address, mailId)
        val item = result.item()
        assertEquals(address, item["address"]?.s())
        assertEquals(mailId, item["mailId"]?.s())
    }

    @Test
    fun query(): Unit = runBlocking {
        val results = repository.query(address)
        assertEquals(1, results.count())
    }

    @AfterAll
    fun tearDown(): Unit = runBlocking {
        repository.delete(address, mailId)
    }

}
