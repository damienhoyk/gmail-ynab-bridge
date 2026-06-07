package noodle.ynabsync.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromM
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import java.util.UUID

@TestMethodOrder(OrderAnnotation::class)
@TestInstance(PER_CLASS)
class DynamoDbBridgeRepositoryTests {
    private val repository = DynamoDbBridgeRepository(environment = "test")
    private val mailAddress = "test-${UUID.randomUUID()}@gmail.com"
    private val destination = UUID.randomUUID().toString()

    @Order(1)
    @Test
    fun put(): Unit =
        runBlocking {
            repository.put(mailAddress, destination) {
                put("accounts", fromM(mapOf("acc-1" to fromS("ynab-1"))))
            }
        }

    @Test
    fun getAccounts(): Unit =
        runBlocking {
            val result = repository.getAccounts(mailAddress, destination)
            assertEquals(mapOf("acc-1" to "ynab-1"), result)
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(mailAddress, destination)
        }
}
