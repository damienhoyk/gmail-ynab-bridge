package noodle.finance.port.`in`

import noodle.email.domain.GmailMessageRequest
import noodle.email.domain.GmailMessageRequest.Format
import noodle.finance.domain.SyncYnabCommand
import noodle.finance.domain.YnabTransactionsRequest
import noodle.finance.port.out.BridgeRepository
import noodle.finance.port.out.GmailClientFactory
import noodle.finance.port.out.MatcherRepository
import noodle.finance.port.out.OutboxRepository
import noodle.finance.port.out.YnabClientFactory
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours

class YnabEmailService(
    val ynabClientFactory: suspend () -> YnabClientFactory,
    val gmailClientFactory: suspend () -> GmailClientFactory,
    val bridgeRepository: suspend () -> BridgeRepository,
    val matcherRepository: suspend () -> MatcherRepository,
    val outboxRepository: suspend () -> OutboxRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun execute(command: SyncYnabCommand) {
        val destination = command.destination
        val mailAddress = command.mailAddress
        val mailId = command.mailId
        val source = command.source

        if (destination.isNullOrEmpty()) {
            log.error("Invalid destination")
            return
        }

        if (!destination.endsWith("@app.ynab.com", ignoreCase = true)) {
            log.info("Filter destination [{}]", destination)
            return
        }

        if (source.isNullOrEmpty()) {
            log.error("Invalid source")
            return
        }

        if (mailId.isNullOrEmpty()) {
            log.error("Invalid mailId")
            return
        }

        if (mailAddress.isNullOrEmpty()) {
            log.error("Invalid mailAddress")
            return
        }

        val ynabClientFactory = ynabClientFactory()

        val message =
            when {
                mailAddress.endsWith("@gmail.com", ignoreCase = true) -> {
                    val gmailClientFactory = gmailClientFactory()
                    val client = gmailClientFactory.create(mailAddress)
                    val request = GmailMessageRequest(mailId, Format.FULL)
                    client.getMessage(request)
                }
                else -> throw IllegalStateException("unknown mail provider")
            }

        when (message.status) {
            200 -> outboxRepository().updateTtl(destination, source, 24.hours)
            404 -> outboxRepository().updateTtl(destination, source, 1.hours)
            else -> outboxRepository().updateTtl(destination, source, 120.hours)
        }

        if (message.status?.equals(200) == false) {
            log.error("Failed to get message [{}]", source)
            return
        }

        log.info("Getting bridge for [{}|{}] ...", mailAddress, destination)

        val bridgeRepository = bridgeRepository()
        val bridge = bridgeRepository.getBridge(mailAddress, destination)

        val client = ynabClientFactory.create(destination)
        val accounts = bridge.accounts ?: emptyMap()

        log.info("Bridge has [{}] accounts", accounts.size)

        log.info("Getting matchers for [{}] ...", message.senderEmail)

        val matcherRepository = matcherRepository()
        val matchers = matcherRepository.queryMatcher(message.senderEmail!!)

        log.info("Got [{}] matchers", matchers.count())

        val transaction = matchers.firstNotNullOfOrNull { it.parse(message.text!!) }

        if (transaction == null) {
            log.warn(
                "⚠️ Did not extract any transaction from message [{}|{} ...]",
                mailId,
                message.text?.take(50),
            )
            outboxRepository().updateTtl(destination, source, 1.hours)
            return
        }

        val ynabTransaction =
            YnabTransactionsRequest.YnabTransaction(
                id = transaction.id,
                accountId = accounts[transaction.accountId],
                amount = transaction.amount,
                date = transaction.date,
                payeeName = transaction.payeeName,
            )

        log.info("Sending request to [{}]", destination)

        client.postTransactions(transactions = listOf(ynabTransaction))

        outboxRepository().updateTtl(destination, source, 24.hours)
    }
}
