package noodle.finance.port.`in`

import jakarta.mail.internet.InternetAddress
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
import kotlin.text.equals
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

        val messageResponse =
            when {
                mailAddress.endsWith("@gmail.com", ignoreCase = true) -> {
                    val gmailClientFactory = gmailClientFactory()
                    val client = gmailClientFactory.create(mailAddress)
                    val request = GmailMessageRequest(mailId, Format.FULL)
                    client.getMessage(request)
                }
                else -> throw IllegalStateException("unknown mail provider")
            }

        when (messageResponse.status) {
            403, 404, 410 -> outboxRepository().updateTtl(destination, source, 1.hours)
            else -> outboxRepository().updateTtl(destination, source, 120.hours)
        }

        if (messageResponse.status?.equals(200) == false) {
            log.error("Invalid response")
            return
        }

        val (fromAddress, messageText) =
            when {
                mailAddress.endsWith("@gmail.com", ignoreCase = true) -> {
                    val message = messageResponse
                    val messageText = message.text
                    val messageHeaders = message.payload?.headers

                    val fromHeader = messageHeaders?.find { it["name"].equals("from", true) }
                    val fromValue = fromHeader?.get("value")

                    val fromAddress = InternetAddress(fromValue).address

                    fromAddress to messageText
                }
                else -> throw IllegalStateException("unknown mail provider")
            }

        log.info("Getting bridge for [{}|{}] ...", mailAddress, destination)

        val bridgeRepository = bridgeRepository()
        val bridge = bridgeRepository.getBridge(mailAddress, destination)

        val client = ynabClientFactory.create(destination)
        val accounts = bridge.accounts ?: emptyMap()

        log.info("Bridge has [{}] accounts", accounts.size)

        log.info("Getting matchers for [{}] ...", fromAddress)

        val matcherRepository = matcherRepository()
        val matchers = matcherRepository.queryMatcher(fromAddress)

        log.info("Got [{}] matchers", matchers.count())

        val transaction = matchers.firstNotNullOfOrNull { it.parse(messageText) }

        if (transaction == null) {
            log.warn(
                "⚠️ Did not extract any transaction from message [{}|{} ...]",
                mailId,
                messageText.take(50),
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
