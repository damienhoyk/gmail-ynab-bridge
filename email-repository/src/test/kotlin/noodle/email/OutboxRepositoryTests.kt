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
class OutboxRepositoryTests {

    val repository = OutboxRepository(environment = "test")
    val destination = "${UUID.randomUUID()}@app.ynab.com"
    val address = "test-${UUID.randomUUID()}@gmail.com"
    val mailId = UUID.randomUUID().toString()
    val source = "$mailId:$address"

    @Order(1)
    @Test
    fun put(): Unit = runBlocking {
        repository.put(destination, source)
    }

    @Test
    fun get(): Unit = runBlocking {
        val result = repository.get(destination, source)
        val item = result.item()
        assertEquals(destination, item["destination"]?.s())
        assertEquals(source, item["source"]?.s())
    }

    @Test
    fun query(): Unit = runBlocking {
        val results = repository.query(destination)
        assertEquals(1, results.count())
    }

    @AfterAll
    fun tearDown(): Unit = runBlocking {
        repository.delete(destination, source)
    }

}
