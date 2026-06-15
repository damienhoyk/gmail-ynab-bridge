package noodle.ynabsync.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import noodle.ynabsync.core.domain.Account
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
class DynamoDbAccountRepositoryTests {
    private val repository = DynamoDbAccountRepository(environment = "test")
    private val userId = UUID.randomUUID()
    private val owner = "urn:app.ynab.com:test-$userId"
    private val accountId = "urn:app.ynab.com:test-$userId:budget:test-budget:account:test-account"
    private val bankAccount = "1995"

    private val malformedUserId = UUID.randomUUID()
    private val malformedOwner = "urn:app.ynab.com:malformed-test-$malformedUserId"
    private val malformedGoodAccountId = "urn:app.ynab.com:malformed-test-$malformedUserId:budget:malformed-test-budget:account:malformed-test-account"

    private val missingBankUserId = UUID.randomUUID()
    private val missingBankOwner = "urn:app.ynab.com:missing-bank-test-$missingBankUserId"
    private val missingBankGoodAccountId = "urn:app.ynab.com:missing-bank-test-$missingBankUserId:budget:missing-bank-test-budget:account:missing-bank-test-account"
    private val noBankAccountId = "urn:app.ynab.com:missing-bank-test-$missingBankUserId-2:budget:missing-bank-test-budget-2:account:missing-bank-test-account-2"

    @Order(1)
    @Test
    fun put(): Unit =
        runBlocking {
            repository.put(owner, accountId) {
                put("bankAccount", fromS(bankAccount))
            }
        }

    @Test
    fun getAccounts(): Unit =
        runBlocking {
            val result = repository.getAccounts(owner)
            assertEquals(1, result.size)
            val account = result[0]
            val expectedUrn = YnabUrn("test-$userId", "test-budget", "test-account")
            assertEquals(Account(bankAccount, expectedUrn), account)
        }

    @Test
    fun skipsMalformedUrn(): Unit =
        runBlocking {
            val goodBankAccount = "2001"
            val malformedAccountId = "not-a-valid-urn"

            repository.put(malformedOwner, malformedGoodAccountId) {
                put("bankAccount", fromS(goodBankAccount))
            }
            repository.put(malformedOwner, malformedAccountId) {
                put("bankAccount", fromS("9999"))
            }

            val result = repository.getAccounts(malformedOwner)
            val expectedUrn = YnabUrn("malformed-test-$malformedUserId", "malformed-test-budget", "malformed-test-account")
            val expectedAccount = Account(goodBankAccount, expectedUrn)
            assertEquals(expectedAccount, result.single())

            repository.delete(malformedOwner, malformedGoodAccountId)
        }

    @Test
    fun skipsMissingBankAccount(): Unit =
        runBlocking {
            val goodBankAccount = "2002"

            repository.put(missingBankOwner, missingBankGoodAccountId) {
                put("bankAccount", fromS(goodBankAccount))
            }
            repository.put(missingBankOwner, noBankAccountId)

            val result = repository.getAccounts(missingBankOwner)
            val expectedUrn = YnabUrn("missing-bank-test-$missingBankUserId", "missing-bank-test-budget", "missing-bank-test-account")
            val expectedAccount = Account(goodBankAccount, expectedUrn)
            assertEquals(expectedAccount, result.single())

            repository.delete(missingBankOwner, missingBankGoodAccountId)
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(owner, accountId)
        }
}
