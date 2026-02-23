package noodle.repository

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BridgeRepositoryTests {

    val repository = BridgeRepository()

    val source = "damien.hoyk+test@gmail.com"
    val destination = "test-ynab-id"

    @Order(1)
    @Test
    fun updateStatus(): Unit = runBlocking {
        val item = repository.updateStatus(source, destination, "test")
        println(item.attributes())
    }

    @Order(2)
    @Test
    fun updateStatusMutex(): Unit = runBlocking {
        val item = repository.updateStatusMutex(source, destination, "running")
        println(item.attributes())
    }

}