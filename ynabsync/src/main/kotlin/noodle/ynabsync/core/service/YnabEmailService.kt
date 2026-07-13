package noodle.ynabsync.core.service

import noodle.ynabsync.core.domain.MailMessageRequest
import noodle.ynabsync.core.domain.MailMessageRequest.Format
import noodle.ynabsync.core.domain.SyncYnabCommand
import noodle.ynabsync.core.port.BankAccountRepository
import noodle.ynabsync.core.port.GmailClientFactory
import noodle.ynabsync.core.port.LoginRepository
import noodle.ynabsync.core.port.MatcherRepository
import noodle.ynabsync.core.port.OutboxRepository
import noodle.ynabsync.core.port.YnabClientFactory
import org.slf4j.LoggerFactory
import java.net.URI
import kotlin.time.Duration.Companion.hours

public class YnabEmailService(
    public val ynabClientFactory: suspend () -> YnabClientFactory,
    public val gmailClientFactory: suspend () -> GmailClientFactory,
    public val accountRepository: suspend () -> BankAccountRepository,
    public val matcherRepository: suspend () -> MatcherRepository,
    public val outboxRepository: suspend () -> OutboxRepository,
    public val loginRepository: suspend () -> LoginRepository,
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

        val domain = mailAddress.substringAfter('@')
        val message =
            when (domain) {
                "gmail.com" -> {
                    val gmailClientFactory = gmailClientFactory()
                    val loginRepository = loginRepository()
                    val loginId =
                        loginRepository.resolve(mailAddress) ?: run {
                            log.error("No Google login for [{}]", mailAddress)
                            return
                        }
                    val client = gmailClientFactory.create(loginId)
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

        val uri =
            runCatching {
                URI(destination)
            }.getOrElse {
                log.error("Failed to parse destination URI [{}]", destination, it)
                return
            }

        val userId =
            uri.userInfo ?: run {
                log.error("No userInfo in destination URI [{}]", destination)
                return
            }

        val loginId = "//$userId@${uri.host}"
        val client = ynabClientFactory.create(loginId)

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

        val bankAccountNumber =
            transaction.accountId
                ?: run {
                    log.error("Transaction has no accountId")
                    return
                }

        val accountRepository = accountRepository()
        val ynabAccounts = accountRepository.getAccounts(mailAddress, bankAccountNumber)
        if (ynabAccounts.isEmpty()) {
            runCatching {
                accountRepository.putDiscoveredAccount(mailAddress, bankAccountNumber, userId)
            }.onFailure { e ->
                log.warn("Failed to store discovered account [{}|{}]", bankAccountNumber, userId, e)
            }
            log.error("No account mapping for [{}]", bankAccountNumber)
            return
        }

        log.info("Sending request to [{}] for [{}] distinct accounts", destination, ynabAccounts.distinct().size)

        ynabAccounts.distinct().forEach { account ->
            val ynabTransaction = transaction.copy(accountId = account.accountId)
            client.postTransactions(budgetId = account.budgetId, transactions = listOf(ynabTransaction))
        }

        outboxRepository.updateTtl(destination, source, TTL_SUCCESS)
    }
}
