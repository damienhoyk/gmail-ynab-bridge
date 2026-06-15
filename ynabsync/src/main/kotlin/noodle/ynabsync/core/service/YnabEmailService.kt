package noodle.ynabsync.core.service

import noodle.ynabsync.core.domain.MailMessageRequest
import noodle.ynabsync.core.domain.MailMessageRequest.Format
import noodle.ynabsync.core.domain.SyncYnabCommand
import noodle.ynabsync.core.port.AccountRepository
import noodle.ynabsync.core.port.GmailClientFactory
import noodle.ynabsync.core.port.MatcherRepository
import noodle.ynabsync.core.port.OutboxRepository
import noodle.ynabsync.core.port.YnabClientFactory
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours

public class YnabEmailService(
    public val ynabClientFactory: suspend () -> YnabClientFactory,
    public val gmailClientFactory: suspend () -> GmailClientFactory,
    public val accountRepository: suspend () -> AccountRepository,
    public val matcherRepository: suspend () -> MatcherRepository,
    public val outboxRepository: suspend () -> OutboxRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    public companion object {
        private val TTL_SUCCESS = 24.hours
        private val TTL_NOT_FOUND = 1.hours
    }

    public suspend fun execute(command: SyncYnabCommand) {
        val destination = command.destination
        val mailAddress = command.mailAddress
        val mailId = command.mailId
        val source = command.source

        val ynabClientFactory = ynabClientFactory()
        val outboxRepository = outboxRepository()

        val message =
            when {
                mailAddress.endsWith("@gmail.com", ignoreCase = true) -> {
                    val gmailClientFactory = gmailClientFactory()
                    val client = gmailClientFactory.create(mailAddress)
                    val request = MailMessageRequest(mailId, Format.FULL)
                    client.getMessage(request)
                }
                else -> throw IllegalStateException("unknown mail provider")
            }

        if (message.status == 404) {
            log.error("Failed to get message [{}]", source)
            outboxRepository.updateTtl(destination, source, TTL_NOT_FOUND)
            return
        }

        if (message.status != 200) {
            log.error("Failed to get message [{}] status=[{}]", source, message.status)
            return
        }

        val senderEmail =
            message.senderEmail ?: run {
                log.error("Message [{}] has no sender email", source)
                return
            }

        val text =
            message.text ?: run {
                log.error("Message [{}] has no text", source)
                return
            }

        val userId = destination.removePrefix("urn:app.ynab.com:").substringBefore(":")
        val client = ynabClientFactory.create("$userId@app.ynab.com")

        val accountRepository = accountRepository()
        val accounts = accountRepository.getAccounts(destination)

        val accountsByBank = accounts.groupBy({ it.bankAccount }, { it.ynabAccount })

        log.info("Getting matchers for [{}] ...", senderEmail)

        val matcherRepository = matcherRepository()
        val matchers = matcherRepository.queryMatcher(senderEmail)

        log.info("Got [{}] matchers", matchers.count())

        val transaction = matchers.firstNotNullOfOrNull { it.parse(text) }

        if (transaction == null) {
            log.warn(
                "⚠️ Did not extract any transaction from message [{}|{} ...]",
                mailId,
                text.take(50),
            )
            return
        }

        val ynabAccounts =
            accountsByBank[transaction.accountId]
                ?: run {
                    log.error("No account mapping for [{}]", transaction.accountId)
                    return
                }

        log.info("Sending request to [{}] for [{}] distinct accounts", destination, ynabAccounts.distinct().size)

        ynabAccounts.distinct().forEach { urn ->
            val ynabTransaction = transaction.copy(accountId = urn.accountId)
            client.postTransactions(budgetId = urn.budgetId, transactions = listOf(ynabTransaction))
        }

        outboxRepository.updateTtl(destination, source, TTL_SUCCESS)
    }
}
