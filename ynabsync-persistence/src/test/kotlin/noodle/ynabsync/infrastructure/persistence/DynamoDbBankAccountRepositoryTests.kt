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
import java.util.UUID

@TestMethodOrder(OrderAnnotation::class)
@TestInstance(PER_CLASS)
class DynamoDbBankAccountRepositoryTests {
    private val repository = DynamoDbBankAccountRepository(environment = "test")
    private val testEmail = "e@gmail.com"
    private val testNumber = "9062"
    private val testUserId = "test-user-${UUID.randomUUID()}"
    private val testBudgetId = "test-budget"
    private val testAccountId = "test-account"
    private val testPartition = "noodle.ynabsync://$testEmail/account/$testNumber"
    private val testSort = "noodle.ynabsync://$testUserId@app.ynab.com/budget/$testBudgetId/account/$testAccountId"

    @Order(1)
    @Test
    fun put(): Unit =
        runBlocking {
            repository.put(testPartition, testSort)
        }

    @Test
    fun getAccounts(): Unit =
        runBlocking {
            val result = repository.getAccounts(testEmail, testNumber)
            assertEquals(1, result.size)
            val account = result[0]
            assertEquals(testEmail, account.email)
            assertEquals(testNumber, account.number)
            assertEquals(testUserId, account.userId)
            assertEquals(testBudgetId, account.budgetId)
            assertEquals(testAccountId, account.accountId)
        }

    @Test
    fun skipsMalformedUri(): Unit =
        runBlocking {
            val badEmail = "malformed@gmail.com"
            val badNumber = "9999"
            val badPartition = "noodle.ynabsync://$badEmail/account/$badNumber"

            val goodSort = "noodle.ynabsync://good-user-${UUID.randomUUID()}@app.ynab.com/budget/good-budget/account/good-account"
            val malformedSort = "not-a-valid-uri"

            // Put one good and one malformed sort key
            repository.put(badPartition, goodSort)
            repository.put(badPartition, malformedSort)

            val result = repository.getAccounts(badEmail, badNumber)
            // Should skip the malformed one and return only the valid one
            assertEquals(1, result.size)
            assertEquals(badEmail, result[0].email)
            assertEquals(badNumber, result[0].number)

            repository.delete(badPartition, goodSort)
        }

    @Test
    fun skipsWrongScheme(): Unit =
        runBlocking {
            val badEmail = "wrong-scheme@gmail.com"
            val badNumber = "8888"
            val badPartition = "noodle.ynabsync://$badEmail/account/$badNumber"

            val wrongSchemeSort = "http://user@app.ynab.com/budget/my-budget/account/acc-123"

            repository.put(badPartition, wrongSchemeSort)

            val result = repository.getAccounts(badEmail, badNumber)
            // Should skip wrong scheme and return empty
            assertEquals(0, result.size)

            repository.delete(badPartition, wrongSchemeSort)
        }

    @Test
    fun skipsWrongHost(): Unit =
        runBlocking {
            val badEmail = "wrong-host@gmail.com"
            val badNumber = "7777"
            val badPartition = "noodle.ynabsync://$badEmail/account/$badNumber"

            val wrongHostSort = "noodle.ynabsync://user@example.com/budget/my-budget/account/acc-123"

            repository.put(badPartition, wrongHostSort)

            val result = repository.getAccounts(badEmail, badNumber)
            // Should skip wrong host and return empty
            assertEquals(0, result.size)

            repository.delete(badPartition, wrongHostSort)
        }

    @Test
    fun skipsMissingUserInfo(): Unit =
        runBlocking {
            val badEmail = "missing-user@gmail.com"
            val badNumber = "6666"
            val badPartition = "noodle.ynabsync://$badEmail/account/$badNumber"

            val missingUserSort = "noodle.ynabsync://app.ynab.com/budget/my-budget/account/acc-123"

            repository.put(badPartition, missingUserSort)

            val result = repository.getAccounts(badEmail, badNumber)
            // Should skip missing userInfo and return empty
            assertEquals(0, result.size)

            repository.delete(badPartition, missingUserSort)
        }

    @Test
    fun skipsNonMatchingPath(): Unit =
        runBlocking {
            val badEmail = "bad-path@gmail.com"
            val badNumber = "5555"
            val badPartition = "noodle.ynabsync://$badEmail/account/$badNumber"

            val badPathSort = "noodle.ynabsync://user@app.ynab.com/notbudget/my-budget/notaccount/acc-123"

            repository.put(badPartition, badPathSort)

            val result = repository.getAccounts(badEmail, badNumber)
            // Should skip non-matching path and return empty
            assertEquals(0, result.size)

            repository.delete(badPartition, badPathSort)
        }

    @Test
    fun skipsBlankPathSegments(): Unit =
        runBlocking {
            val badEmail = "blank-segments@gmail.com"
            val badNumber = "4444"
            val badPartition = "noodle.ynabsync://$badEmail/account/$badNumber"

            // Blank budget segment
            val blankBudgetSort = "noodle.ynabsync://user@app.ynab.com/budget//account/acc-123"
            repository.put(badPartition, blankBudgetSort)

            var result = repository.getAccounts(badEmail, badNumber)
            assertEquals(0, result.size)
            repository.delete(badPartition, blankBudgetSort)

            // Blank account segment
            val blankAccountSort = "noodle.ynabsync://user@app.ynab.com/budget/my-budget/account/"
            repository.put(badPartition, blankAccountSort)

            result = repository.getAccounts(badEmail, badNumber)
            assertEquals(0, result.size)
            repository.delete(badPartition, blankAccountSort)
        }

    @Test
    fun skipsExtraTrailingPathSegments(): Unit =
        runBlocking {
            val badEmail = "extra-segments@gmail.com"
            val badNumber = "3333"
            val badPartition = "noodle.ynabsync://$badEmail/account/$badNumber"

            // Extra path segment
            val extraSegmentSort = "noodle.ynabsync://user@app.ynab.com/budget/my-budget/account/acc-123/extra"
            repository.put(badPartition, extraSegmentSort)

            var result = repository.getAccounts(badEmail, badNumber)
            assertEquals(0, result.size)
            repository.delete(badPartition, extraSegmentSort)

            // Trailing slash
            val trailingSlashSort = "noodle.ynabsync://user@app.ynab.com/budget/my-budget/account/acc-123/"
            repository.put(badPartition, trailingSlashSort)

            result = repository.getAccounts(badEmail, badNumber)
            assertEquals(0, result.size)
            repository.delete(badPartition, trailingSlashSort)
        }

    @Test
    fun acceptsCaseInsensitiveSchemeAndHost(): Unit =
        runBlocking {
            val goodEmail = "case-insensitive@gmail.com"
            val goodNumber = "2222"
            val goodPartition = "noodle.ynabsync://$goodEmail/account/$goodNumber"
            val userId = "case-user-${UUID.randomUUID()}"

            // UPPERCASE scheme and host
            val uppercaseSort = "NOODLE.YNABSYNC://$userId@APP.YNAB.COM/budget/case-budget/account/case-account"
            repository.put(goodPartition, uppercaseSort)

            val result = repository.getAccounts(goodEmail, goodNumber)
            // Should accept case-insensitive scheme and host
            assertEquals(1, result.size)
            val account = result[0]
            assertEquals(goodEmail, account.email)
            assertEquals(goodNumber, account.number)
            assertEquals(userId, account.userId)
            assertEquals("case-budget", account.budgetId)
            assertEquals("case-account", account.accountId)

            repository.delete(goodPartition, uppercaseSort)
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(testPartition, testSort)
        }
}
