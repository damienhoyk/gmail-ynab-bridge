package noodle.ynabsync.core.service

import noodle.ynabsync.core.domain.MailMessageRequest
import noodle.ynabsync.core.domain.MailMessageRequest.Format
import noodle.ynabsync.core.domain.SyncYnabCommand
import noodle.ynabsync.core.port.BridgeRepository
import noodle.ynabsync.core.port.GmailClientFactory
import noodle.ynabsync.core.port.MatcherRepository
import noodle.ynabsync.core.port.OutboxRepository
import noodle.ynabsync.core.port.YnabClientFactory
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours

public class YnabEmailService(
    public val ynabClientFactory: suspend () -> YnabClientFactory,
    public val gmailClientFactory: suspend () -> GmailClientFactory,
    public val bridgeRepository: suspend () -> BridgeRepository,
    public val matcherRepository: suspend () -> MatcherRepository,
    public val outboxRepository: suspend () -> OutboxRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    public companion object {
        private val TTL_SUCCESS = 24.hours
        private val TTL_NOT_FOUND = 1.hours
        private val TTL_ERROR = 120.hours
        private val TTL_NO_MATCH = 1.hours
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

        val ttl =
            when (message.status) {
                200 -> TTL_SUCCESS
                404 -> TTL_NOT_FOUND
                else -> TTL_ERROR
            }
        outboxRepository.updateTtl(destination, source, ttl)

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

        log.info("Getting bridge for [{}|{}] ...", mailAddress, destination)

        val bridgeRepository = bridgeRepository()
        val client = ynabClientFactory.create(destination)
        val accounts = bridgeRepository.getAccounts(mailAddress, destination)

        log.info("Bridge has [{}] accounts", accounts.size)

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
            outboxRepository.updateTtl(destination, source, TTL_NO_MATCH)
            return
        }

        val accountId =
            accounts[transaction.accountId]
                ?: run {
                    log.error("No account mapping for [{}]", transaction.accountId)
                    outboxRepository.updateTtl(destination, source, TTL_ERROR)
                    return
                }

        val ynabTransaction = transaction.copy(accountId = accountId)

        log.info("Sending request to [{}]", destination)

        client.postTransactions(transactions = listOf(ynabTransaction))

        outboxRepository.updateTtl(destination, source, TTL_SUCCESS)
    }
}
