package noodle.ynabsync.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import noodle.ynabsync.core.domain.Bridge
import noodle.ynabsync.core.domain.YnabUrn
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
class DynamoDbBridgeRepositoryTests {
    private val repository = DynamoDbBridgeRepository(environment = "test")
    private val mailAddress = "test-${UUID.randomUUID()}@gmail.com"
    private val destination = "urn:app.ynab.com:test-user:budget:test-budget:account:test-account"
    private val bankAccount = "1995"

    @Order(1)
    @Test
    fun put(): Unit =
        runBlocking {
            repository.put(mailAddress, destination) {
                put("bankAccount", fromS(bankAccount))
            }
        }

    @Test
    fun getBridges(): Unit =
        runBlocking {
            val result = repository.getBridges(mailAddress)
            assertEquals(1, result.size)
            val bridge = result[0]
            val expectedUrn = YnabUrn("test-user", "test-budget", "test-account")
            assertEquals(Bridge(bankAccount, expectedUrn), bridge)
        }

    @Test
    fun skipsMalformedUrn(): Unit =
        runBlocking {
            val malformedMailAddress = "malformed-urn-test-${UUID.randomUUID()}@gmail.com"
            val goodDestination = "urn:app.ynab.com:malformed-test-user:budget:malformed-test-budget:account:malformed-test-account"
            val goodBankAccount = "2001"
            val malformedDestination = "not-a-valid-urn"

            // Write one good bridge
            repository.put(malformedMailAddress, goodDestination) {
                put("bankAccount", fromS(goodBankAccount))
            }
            // Write one malformed bridge that should be skipped
            repository.put(malformedMailAddress, malformedDestination) {
                put("bankAccount", fromS("9999"))
            }

            val result = repository.getBridges(malformedMailAddress)
            val expectedUrn = YnabUrn("malformed-test-user", "malformed-test-budget", "malformed-test-account")
            val expectedBridge = Bridge(goodBankAccount, expectedUrn)
            assertEquals(expectedBridge, result.single())

            // Cleanup
            repository.delete(malformedMailAddress, goodDestination)
        }

    @Test
    fun skipsMissingBankAccount(): Unit =
        runBlocking {
            val missingBankMailAddress = "missing-bank-test-${UUID.randomUUID()}@gmail.com"
            val goodDestination = "urn:app.ynab.com:missing-bank-test-user:budget:missing-bank-test-budget:account:missing-bank-test-account"
            val goodBankAccount = "2002"
            val noBankAccountDestination = "urn:app.ynab.com:missing-bank-test-user-2:budget:missing-bank-test-budget-2:account:missing-bank-test-account-2"

            // Write one good bridge
            repository.put(missingBankMailAddress, goodDestination) {
                put("bankAccount", fromS(goodBankAccount))
            }
            // Write one bridge without bankAccount that should be skipped
            repository.put(missingBankMailAddress, noBankAccountDestination)

            val result = repository.getBridges(missingBankMailAddress)
            val expectedUrn = YnabUrn("missing-bank-test-user", "missing-bank-test-budget", "missing-bank-test-account")
            val expectedBridge = Bridge(goodBankAccount, expectedUrn)
            assertEquals(expectedBridge, result.single())

            // Cleanup
            repository.delete(missingBankMailAddress, goodDestination)
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(mailAddress, destination)
        }
}
