package noodle.ynabsync.core.service

import kotlinx.coroutines.runBlocking
import noodle.ynabsync.core.domain.Bridge
import noodle.ynabsync.core.domain.GmailMessage
import noodle.ynabsync.core.domain.MailMessageRequest
import noodle.ynabsync.core.domain.SyncYnabCommand
import noodle.ynabsync.core.domain.TransactionMatcher
import noodle.ynabsync.core.domain.TransactionMatcher.RegexGroup
import noodle.ynabsync.core.domain.YnabTransaction
import noodle.ynabsync.core.domain.YnabUrn
import noodle.ynabsync.core.port.BridgeRepository
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
    private val testDestination = "$testUserId@app.ynab.com"
    private val testMailId = "mail-id-456"
    private val testSource = "source-789"
    private val testBankAccount = "1995"
    private val testBudgetId = "my-budget"
    private val testYnabAccountId = "ynab-acc-999"
    private val testYnabUrn = YnabUrn(testUserId, testBudgetId, testYnabAccountId)

    @Test
    fun happyPath(): Unit =
        runBlocking {
            // Arrange
            val fakeYnabClient = FakeYnabClient()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient)
            val fakeGmailClient = FakeGmailClient()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeBridgeRepository =
                FakeBridgeRepository(
                    listOf(
                        Bridge(testBankAccount, testYnabUrn),
                    ),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    bridgeRepository = { fakeBridgeRepository },
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

            assertEquals(testDestination, fakeOutboxRepository.lastUpdateTtlDestination)
            assertEquals(testSource, fakeOutboxRepository.lastUpdateTtlSource)
            assertEquals(24.hours, fakeOutboxRepository.lastUpdateTtlDuration)
        }

    @Test
    fun filtersBridgesFromDifferentYnabUser(): Unit =
        runBlocking {
            // Arrange - bridge with a different YNAB user ID
            val otherUserUrn = YnabUrn("other-user", "other-budget", "other-account")
            val fakeYnabClient = FakeYnabClient()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient)
            val fakeGmailClient = FakeGmailClient()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeBridgeRepository =
                FakeBridgeRepository(
                    listOf(
                        Bridge(testBankAccount, otherUserUrn),
                    ),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    bridgeRepository = { fakeBridgeRepository },
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
    fun logsAndReturnsWhenNoBridgeFound(): Unit =
        runBlocking {
            // Arrange
            val fakeYnabClient = FakeYnabClient()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient)
            val fakeGmailClient = FakeGmailClient()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeBridgeRepository =
                FakeBridgeRepository(
                    listOf(
                        Bridge("different-bank-account", testYnabUrn),
                    ),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    bridgeRepository = { fakeBridgeRepository },
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
    fun updatesOutboxWithSuccessTtlOnSuccess(): Unit =
        runBlocking {
            // Arrange
            val fakeYnabClient = FakeYnabClient()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient)
            val fakeGmailClient = FakeGmailClient()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeBridgeRepository =
                FakeBridgeRepository(
                    listOf(
                        Bridge(testBankAccount, testYnabUrn),
                    ),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    bridgeRepository = { fakeBridgeRepository },
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
            // Arrange - two bridge rows with the SAME user, SAME bankAccount, but DIFFERENT account IDs
            // This should fan-out: the transaction posts to BOTH YNAB accounts
            val otherYnabAccountId = "ynab-acc-888"
            val otherUrn = YnabUrn(testUserId, testBudgetId, otherYnabAccountId)
            val fakeYnabClient = FakeYnabClient()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient)
            val fakeGmailClient = FakeGmailClient()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeBridgeRepository =
                FakeBridgeRepository(
                    listOf(
                        // Same bank account, same YNAB user, but different account IDs — fan-out!
                        Bridge(testBankAccount, testYnabUrn),
                        Bridge(testBankAccount, otherUrn),
                    ),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    bridgeRepository = { fakeBridgeRepository },
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
            // Arrange - two bridge rows with the SAME user, SAME bankAccount, IDENTICAL URN (idempotent duplicate)
            // The dedup logic should ensure the transaction posts EXACTLY ONCE
            val fakeYnabClient = FakeYnabClient()
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient)
            val fakeGmailClient = FakeGmailClient()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeBridgeRepository =
                FakeBridgeRepository(
                    listOf(
                        // Same bank account, same URN — idempotent duplicate, should dedup
                        Bridge(testBankAccount, testYnabUrn),
                        Bridge(testBankAccount, testYnabUrn),
                    ),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    bridgeRepository = { fakeBridgeRepository },
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
            val fakeYnabClientFactory = FakeYnabClientFactory(fakeYnabClient)
            val fakeGmailClient = FakeGmailClient404()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient)
            val fakeBridgeRepository =
                FakeBridgeRepository(
                    listOf(
                        Bridge(testBankAccount, testYnabUrn),
                    ),
                )
            val fakeMatcherRepository = FakeMatcherRepository()
            val fakeOutboxRepository = FakeOutboxRepository()

            val service =
                YnabEmailService(
                    ynabClientFactory = { fakeYnabClientFactory },
                    gmailClientFactory = { fakeGmailClientFactory },
                    bridgeRepository = { fakeBridgeRepository },
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
    ) : YnabClientFactory {
        override suspend fun create(loginId: String): YnabClient = client
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

    private class FakeBridgeRepository(
        private val bridges: List<Bridge>,
    ) : BridgeRepository {
        override suspend fun getBridges(mailAddress: String): List<Bridge> = bridges
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
