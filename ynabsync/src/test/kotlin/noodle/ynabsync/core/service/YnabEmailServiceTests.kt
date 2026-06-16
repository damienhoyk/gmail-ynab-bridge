package noodle.ynabsync.core.service

import kotlinx.coroutines.runBlocking
import noodle.ynabsync.core.domain.BankAccount
import noodle.ynabsync.core.domain.GmailMessage
import noodle.ynabsync.core.domain.MailMessageRequest
import noodle.ynabsync.core.domain.SyncYnabCommand
import noodle.ynabsync.core.domain.TransactionMatcher
import noodle.ynabsync.core.domain.TransactionMatcher.RegexGroup
import noodle.ynabsync.core.domain.YnabTransaction
import noodle.ynabsync.core.port.BankAccountRepository
import noodle.ynabsync.core.port.GmailClient
import noodle.ynabsync.core.port.GmailClientFactory
import noodle.ynabsync.core.port.MatcherRepository
import noodle.ynabsync.core.port.OutboxRepository
import noodle.ynabsync.core.port.YnabClient
import noodle.ynabsync.core.port.YnabClientFactory
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class YnabEmailServiceTests {
    private val testMailAddress = "test@gmail.com"
    private val testUserId = "test-user-123"
    private val testDestination = "noodle.ynabsync://$testUserId@app.ynab.com"
    private val testMailId = "mail-id-456"
    private val testSource = "source-789"
    private val testBankAccount = "1995"
    private val testBudgetId = "my-budget"
    private val testYnabAccountId = "ynab-acc-999"
    private val testBankAccountData =
        BankAccount(
            email = testMailAddress,
            number = testBankAccount,
            userId = testUserId,
            budgetId = testBudgetId,
            accountId = testYnabAccountId,
        )

    @Test
    fun happyPath(): Unit =
        runBlocking {
            // Arrange
            val fakeYnabClient = FakeYnabClient()
            val capturedLoginId = mutableListOf<String>()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient, capturedLoginId)
            val fakeGmailClient = FakeGmailClient()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeAccountRepository =
                FakeBankAccountRepository(
                    mapOf(testMailAddress to mapOf(testBankAccount to listOf(testBankAccountData))),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    accountRepository = { fakeAccountRepository },
                    matcherRepository = { fakeMatcherRepository },
                    outboxRepository = { fakeOutboxRepository },
                )

            val command =
                SyncYnabCommand(
                    destination = testDestination,
                    mailId = testMailId,
                    mailAddress = testMailAddress,
                    source = testSource,
                )

            // Act
            service.execute(command)

            // Assert
            assertEquals(1, fakeYnabClient.allPostTransactionsCalls.size)
            val (budgetId, txns) = fakeYnabClient.allPostTransactionsCalls[0]
            assertEquals(testBudgetId, budgetId)
            assertEquals(testYnabAccountId, txns[0].accountId)

            assertEquals("$testUserId@app.ynab.com", capturedLoginId.single())

            assertEquals(testDestination, fakeOutboxRepository.lastUpdateTtlDestination)
            assertEquals(testSource, fakeOutboxRepository.lastUpdateTtlSource)
            assertEquals(24.hours, fakeOutboxRepository.lastUpdateTtlDuration)
        }

    @Test
    fun noAccountsForBankAccountReturnsWithoutPosting(): Unit =
        runBlocking {
            // Arrange - account mapping exists but yields no results for the bank account
            val fakeYnabClient = FakeYnabClient()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient, mutableListOf())
            val fakeGmailClient = FakeGmailClient()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeAccountRepository =
                FakeBankAccountRepository(
                    mapOf(testMailAddress to mapOf(testBankAccount to listOf())),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    accountRepository = { fakeAccountRepository },
                    matcherRepository = { fakeMatcherRepository },
                    outboxRepository = { fakeOutboxRepository },
                )

            val command =
                SyncYnabCommand(
                    destination = testDestination,
                    mailId = testMailId,
                    mailAddress = testMailAddress,
                    source = testSource,
                )

            // Act
            service.execute(command)

            // Assert - no post should have been made
            assertEquals(null, fakeYnabClient.lastPostTransactionsBudgetId)
            assertEquals(null, fakeYnabClient.lastPostTransactionsInput)
        }

    @Test
    fun returnsEarlyWhenTransactionHasNullAccountId(): Unit =
        runBlocking {
            // Arrange - transaction will have null accountId
            val fakeYnabClient = FakeYnabClient()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient, mutableListOf())
            val fakeGmailClient = FakeGmailClientWithNullAccountId()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeAccountRepository =
                FakeBankAccountRepository(
                    mapOf(testMailAddress to mapOf(testBankAccount to listOf(testBankAccountData))),
                )
            val fakeMatcherRepository = FakeMatcherRepositoryWithNullAccountId()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    accountRepository = { fakeAccountRepository },
                    matcherRepository = { fakeMatcherRepository },
                    outboxRepository = { fakeOutboxRepository },
                )

            val command =
                SyncYnabCommand(
                    destination = testDestination,
                    mailId = testMailId,
                    mailAddress = testMailAddress,
                    source = testSource,
                )

            // Act
            service.execute(command)

            // Assert - should return early without posting or updating outbox
            assertEquals(null, fakeYnabClient.lastPostTransactionsBudgetId)
            assertEquals(null, fakeYnabClient.lastPostTransactionsInput)
            assertEquals(null, fakeOutboxRepository.lastUpdateTtlDestination)
        }

    @Test
    fun updatesOutboxWithSuccessTtlOnSuccess(): Unit =
        runBlocking {
            // Arrange
            val fakeYnabClient = FakeYnabClient()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient, mutableListOf())
            val fakeGmailClient = FakeGmailClient()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeAccountRepository =
                FakeBankAccountRepository(
                    mapOf(testMailAddress to mapOf(testBankAccount to listOf(testBankAccountData))),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    accountRepository = { fakeAccountRepository },
                    matcherRepository = { fakeMatcherRepository },
                    outboxRepository = { fakeOutboxRepository },
                )

            val command =
                SyncYnabCommand(
                    destination = testDestination,
                    mailId = testMailId,
                    mailAddress = testMailAddress,
                    source = testSource,
                )

            // Act
            service.execute(command)

            // Assert
            assertEquals(testDestination, fakeOutboxRepository.lastUpdateTtlDestination)
            assertEquals(testSource, fakeOutboxRepository.lastUpdateTtlSource)
            assertEquals(24.hours, fakeOutboxRepository.lastUpdateTtlDuration)
        }

    @Test
    fun sameUserSameBankAccountDifferentUrnFanOut(): Unit =
        runBlocking {
            // Arrange - two accounts with the SAME bankAccount but DIFFERENT account URNs
            // This should fan-out: the transaction posts to BOTH YNAB accounts
            val otherYnabAccountId = "ynab-acc-888"
            val otherBankAccount =
                BankAccount(
                    email = testMailAddress,
                    number = testBankAccount,
                    userId = testUserId,
                    budgetId = testBudgetId,
                    accountId = otherYnabAccountId,
                )
            val fakeYnabClient = FakeYnabClient()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient, mutableListOf())
            val fakeGmailClient = FakeGmailClient()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeAccountRepository =
                FakeBankAccountRepository(
                    mapOf(testMailAddress to mapOf(testBankAccount to listOf(testBankAccountData, otherBankAccount))),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    accountRepository = { fakeAccountRepository },
                    matcherRepository = { fakeMatcherRepository },
                    outboxRepository = { fakeOutboxRepository },
                )

            val command =
                SyncYnabCommand(
                    destination = testDestination,
                    mailId = testMailId,
                    mailAddress = testMailAddress,
                    source = testSource,
                )

            // Act
            service.execute(command)

            // Assert - transaction should have posted to BOTH YNAB accounts
            assertEquals(2, fakeYnabClient.allPostTransactionsCalls.size)

            // Check that both budget+account pairs appear exactly once
            val (budgetId1, txns1) = fakeYnabClient.allPostTransactionsCalls[0]
            val (budgetId2, txns2) = fakeYnabClient.allPostTransactionsCalls[1]

            assertEquals(testBudgetId, budgetId1)
            assertEquals(testBudgetId, budgetId2)

            val accountIds = setOf(txns1[0].accountId, txns2[0].accountId)
            assertEquals(setOf(testYnabAccountId, otherYnabAccountId), accountIds)
        }

    @Test
    fun sameUserSameBankAccountSameUrnIdempotentDedup(): Unit =
        runBlocking {
            // Arrange - two accounts with the SAME bankAccount and IDENTICAL URN (idempotent duplicate)
            // The dedup logic should ensure the transaction posts EXACTLY ONCE
            val fakeYnabClient = FakeYnabClient()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient, mutableListOf())
            val fakeGmailClient = FakeGmailClient()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeAccountRepository =
                FakeBankAccountRepository(
                    mapOf(testMailAddress to mapOf(testBankAccount to listOf(testBankAccountData, testBankAccountData))),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    accountRepository = { fakeAccountRepository },
                    matcherRepository = { fakeMatcherRepository },
                    outboxRepository = { fakeOutboxRepository },
                )

            val command =
                SyncYnabCommand(
                    destination = testDestination,
                    mailId = testMailId,
                    mailAddress = testMailAddress,
                    source = testSource,
                )

            // Act
            service.execute(command)

            // Assert - the transaction should post EXACTLY ONCE (deduped from 2 identical URNs)
            assertEquals(1, fakeYnabClient.allPostTransactionsCalls.size)
            val (budgetId, txns) = fakeYnabClient.allPostTransactionsCalls[0]
            assertEquals(testBudgetId, budgetId)
            assertEquals(testYnabAccountId, txns[0].accountId)
        }

    @Test
    fun updatesOutboxWithNotFoundTtlWhenMessageNotFound(): Unit =
        runBlocking {
            // Arrange
            val fakeYnabClient = FakeYnabClient()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient, mutableListOf())
            val fakeGmailClient = FakeGmailClient404()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeAccountRepository =
                FakeBankAccountRepository(
                    mapOf(testMailAddress to mapOf(testBankAccount to listOf(testBankAccountData))),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    accountRepository = { fakeAccountRepository },
                    matcherRepository = { fakeMatcherRepository },
                    outboxRepository = { fakeOutboxRepository },
                )

            val command =
                SyncYnabCommand(
                    destination = testDestination,
                    mailId = testMailId,
                    mailAddress = testMailAddress,
                    source = testSource,
                )

            // Act
            service.execute(command)

            // Assert - outbox should be updated with TTL_NOT_FOUND (1 hour)
            assertEquals(testDestination, fakeOutboxRepository.lastUpdateTtlDestination)
            assertEquals(testSource, fakeOutboxRepository.lastUpdateTtlSource)
            assertEquals(1.hours, fakeOutboxRepository.lastUpdateTtlDuration)

            // No transaction should have been posted
            assertEquals(null, fakeYnabClient.lastPostTransactionsBudgetId)
            assertEquals(null, fakeYnabClient.lastPostTransactionsInput)
        }

    // ======== Fakes ========

    private class FakeYnabClientFactory(
        private val client: YnabClient,
        private val capturedLoginIds: MutableList<String>,
    ) : YnabClientFactory {
        override suspend fun create(loginId: String): YnabClient {
            capturedLoginIds.add(loginId)
            return client
        }
    }

    private class FakeYnabClient : YnabClient {
        var lastPostTransactionsBudgetId: String? = null
        var lastPostTransactionsInput: List<YnabTransaction>? = null
        val allPostTransactionsCalls = mutableListOf<Pair<String, List<YnabTransaction>>>()

        override suspend fun postTransactions(
            budgetId: String,
            transactions: List<YnabTransaction>,
        ) {
            lastPostTransactionsBudgetId = budgetId
            lastPostTransactionsInput = transactions
            allPostTransactionsCalls.add(budgetId to transactions)
        }
    }

    private class FakeGmailClientFactory(
        private val client: GmailClient,
    ) : GmailClientFactory {
        override suspend fun create(loginId: String): GmailClient = client
    }

    private class FakeGmailClient : GmailClient {
        override suspend fun getMessage(request: MailMessageRequest): GmailMessage =
            GmailMessage(
                id = "msg-id",
                text = "A transaction of SGD 51.50 was made with your UOB Card ending 1995 on 02/05/25 at Prudential 18031343.",
                senderEmail = "bank@example.com",
                status = 200,
            )
    }

    private class FakeGmailClient404 : GmailClient {
        override suspend fun getMessage(request: MailMessageRequest): GmailMessage =
            GmailMessage(
                id = "msg-id",
                text = null,
                senderEmail = null,
                status = 404,
            )
    }

    private class FakeGmailClientWithNullAccountId : GmailClient {
        override suspend fun getMessage(request: MailMessageRequest): GmailMessage =
            GmailMessage(
                id = "msg-id",
                text = "Some transaction text with no account number",
                senderEmail = "bank@example.com",
                status = 200,
            )
    }

    private class FakeBankAccountRepository(
        private val data: Map<String, Map<String, List<BankAccount>>>,
    ) : BankAccountRepository {
        override suspend fun getAccounts(
            email: String,
            number: String,
        ): List<BankAccount> = data[email]?.get(number) ?: emptyList()
    }

    private class FakeMatcherRepository : MatcherRepository {
        override suspend fun queryMatcher(source: String): List<TransactionMatcher> =
            listOf(
                TransactionMatcher(
                    regex =
                        "A transaction of SGD ([0-9.]+) was made with your UOB Card ending (\\d+) on (\\d{2}/\\d{2}/\\d{2}) at (.+?)\\."
                            .toRegex(),
                    outgoing = true,
                    order = setOf(RegexGroup.AMOUNT, RegexGroup.ACCOUNT, RegexGroup.DATE, RegexGroup.PAYEE),
                    inputDatePattern = "dd/MM/yy",
                ),
            )
    }

    private class FakeMatcherRepositoryWithNullAccountId : MatcherRepository {
        override suspend fun queryMatcher(source: String): List<TransactionMatcher> =
            listOf(
                TransactionMatcher(
                    regex =
                        "A transaction of SGD ([0-9.]+) on (\\d{2}/\\d{2}/\\d{2}) at (.+?)\\."
                            .toRegex(),
                    outgoing = true,
                    order = setOf(RegexGroup.AMOUNT, RegexGroup.DATE, RegexGroup.PAYEE),
                    inputDatePattern = "dd/MM/yy",
                ),
            )
    }

    private class FakeOutboxRepository : OutboxRepository {
        var lastUpdateTtlDestination: String? = null
        var lastUpdateTtlSource: String? = null
        var lastUpdateTtlDuration: Duration? = null

        override suspend fun updateTtl(
            destination: String,
            source: String,
            duration: Duration,
        ): Long {
            lastUpdateTtlDestination = destination
            lastUpdateTtlSource = source
            lastUpdateTtlDuration = duration
            return 1L
        }
    }
}
