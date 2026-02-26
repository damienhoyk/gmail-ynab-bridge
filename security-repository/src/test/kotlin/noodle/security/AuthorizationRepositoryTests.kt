package noodle.security

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import java.util.UUID

@TestMethodOrder(OrderAnnotation::class)
@TestInstance(PER_CLASS)
class AuthorizationRepositoryTests {

    val repository = AuthorizationRepository(environment = "test")
    val id = UUID.randomUUID().toString()
    val refreshToken = UUID.randomUUID().toString()

    @Order(1)
    @Test
    fun put(): Unit = runBlocking {
        val item = mapOf("id" to fromS(id))
        repository.put(id) { put("refreshToken", fromS(refreshToken))}
    }

    @Test
    fun get(): Unit = runBlocking {
        val result = repository.get(id)
        val item = result.item()
        assertEquals(id, item["id"]?.s())
    }

    @AfterAll
    fun tearDown(): Unit = runBlocking {
        repository.delete(id)
    }

}
