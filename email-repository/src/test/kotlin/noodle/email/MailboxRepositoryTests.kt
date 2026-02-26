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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import java.util.UUID

@TestMethodOrder(OrderAnnotation::class)
@TestInstance(PER_CLASS)
class MailboxRepositoryTests {

    val repository = MailboxRepository(environment = "test")
    val address = "test-${UUID.randomUUID()}@gmail.com"
    val state = (100000 .. 199999).random()

    @Order(1)
    @Test
    fun put(): Unit = runBlocking {
        repository.put(address)
    }

    @Test
    fun get(): Unit = runBlocking {
        val result = repository.get(address)
        val item = result.item()
        assertEquals(address, item["address"]?.s())
    }

    @Test
    fun update(): Unit = runBlocking {
        val item = repository.update(address) { put("state", fromN("$state")) }.attributes()
        assertEquals(address, item["address"]?.s())
        assertEquals(state, item["state"]?.n()?.toInt())
    }

    @AfterAll
    fun tearDown(): Unit = runBlocking {
        repository.delete(address)
    }

}
