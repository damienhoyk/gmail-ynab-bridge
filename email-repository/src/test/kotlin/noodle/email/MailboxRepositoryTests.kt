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

    @Order(1)
    @Test
    fun put(): Unit = runBlocking {
        repository.put(address) { put("state", fromN("999"))}
    }

    @Test
    fun get(): Unit = runBlocking {
        val result = repository.get(address)
        val item = result.item()
        assertEquals(address, item["address"]?.s())
    }

    @AfterAll
    fun tearDown(): Unit = runBlocking {
        repository.delete(address)
    }

}
