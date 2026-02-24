package noodle.home.gmail.ynab.job

import io.ktor.client.call.*
import kotlinx.coroutines.*
import noodle.google.gmail.*
import noodle.google.gmail.MessageRequest.Format
import noodle.ynab.Transaction
import noodle.ynab.TransactionsRequest
import noodle.ynab.YnabClient
import org.slf4j.LoggerFactory

class GmailYnabJob(
    private val ynabClient: YnabClient,
    private val googleGmailClient: GoogleGmailClient,
    private val accounts: Map<String, String>,
    private val label: String = "money",
    private val matchers: List<TransactionMatcher>
) {

    val log = LoggerFactory.getLogger(javaClass)

    private val labelId by lazy {
        runBlocking {
            googleGmailClient.getLabels().body<Label.List>().labels
                ?.find { it.name == label }
                ?.id ?: throw IllegalStateException()
        }
    }

    private val historyType = "messageAdded"

    suspend fun run(startHistoryId: Long?) = coroutineScope {
        if (startHistoryId == null) {
            val googleGmailProfile = googleGmailClient.getProfile().body<Profile>()
            val currentHistoryId = googleGmailProfile.historyId
            return@coroutineScope currentHistoryId
        }

        val historyRequest = HistoryRequest(startHistoryId, listOf(historyType), listOf(labelId))
        val history = googleGmailClient.getHistory(request = historyRequest).body<History>()

        val messages = history.messagesAdded
            .mapNotNull { it.message.id }
            .map {
                async(Dispatchers.IO) {
                    googleGmailClient.getMessage(id = it, request = MessageRequest(Format.FULL)).body<Message>()
                }
            }.awaitAll()
            .map { it.text }.filterNot { it.isBlank() }

        if (messages.isNotEmpty()) {
            log.info("👁️ Truncated messages:")
            messages.forEachIndexed { index, message ->
                log.info("$index:\t[{}]", message.take(50))
            }
        }

        val parsed = mutableListOf<Transaction>()
        val unparsed = mutableListOf<String>()

        messages.forEach {
            val transaction = matchers.firstNotNullOfOrNull { matcher -> matcher.parse(it) }

            if (transaction != null) {
                parsed.add(transaction)
            } else {
                unparsed.add(it)
            }
        }

        val (transactions, unmapped) = parsed
            .map { it.copy(accountId = accounts[it.accountId]) }
            .partition { !it.accountId.isNullOrBlank() }

        if (unparsed.isNotEmpty()) {
            log.warn("⚠️ The following messages were not parsed:")
            unparsed.forEachIndexed { index, string ->
                log.info("$index:\t[{}]", string.take(50))
            }
        }

        if (unmapped.isNotEmpty()) {
            log.warn("⚠️ The following transactions did not map to any account:")
            unmapped.forEachIndexed { index, transaction ->
                log.info("$index:\t[{}]", transaction)
            }
        }

        if (transactions.isEmpty()) {
            log.warn("⚠️ There are no transactions to post")
            return@coroutineScope history.historyId
        }

        val body = Transaction.Body(transactions = transactions)
        ynabClient.postTransactions(request = TransactionsRequest(body))

        history.historyId
    }

}