package noodle.email.port.`in`

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import noodle.email.domain.GmailHistoryRequest
import noodle.email.domain.Outbox
import noodle.email.domain.SyncMailboxCommand
import noodle.email.port.out.BridgeRepository
import noodle.email.port.out.GmailClientFactory
import noodle.email.port.out.MailboxRepository
import noodle.email.port.out.OutboxRepository
import noodle.security.port.out.GoogleAuthClient
import org.slf4j.LoggerFactory

class GmailPubsubService(
    private val gmailClientFactory: Deferred<GmailClientFactory>,
    private val googleAuthClient: Deferred<GoogleAuthClient>,
    private val bridgeRepository: Deferred<BridgeRepository>,
    private val mailboxRepository: Deferred<MailboxRepository>,
    private val outboxRepository: Deferred<OutboxRepository>,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val historyType = "messageAdded"

    suspend fun execute(command: SyncMailboxCommand) =
        coroutineScope {
            val bearerToken = command.authorization.substringAfter("Bearer ")

            val state = command.state
            val emailAddress = command.emailAddress

            log.info("📨 Got message from [{}]", emailAddress)

            if (bearerToken.isEmpty()) {
                return@coroutineScope 400
            }

            val googleAuthClient = googleAuthClient.await()
            val tokenInfoAsync = async { googleAuthClient.getTokenInfo(bearerToken) }

            val mailboxRepository = mailboxRepository.await()
            val mailboxAsync = async { mailboxRepository.getMailbox(emailAddress) }

            val gmailClientFactory = gmailClientFactory.await()
            val googleGmailClient = gmailClientFactory.create(emailAddress)

            val mailbox = mailboxAsync.await()
            val mailboxState = mailbox.state

            if (mailboxState == null) {
                log.warn("invalid mailbox state")
                return@coroutineScope 500
            }

            val historyRequest = GmailHistoryRequest(mailboxState, listOf(historyType))
            val history = googleGmailClient.getHistory(request = historyRequest)

            val tokenInfo = tokenInfoAsync.await()

            if (tokenInfo.email.isNullOrEmpty()) {
                return@coroutineScope 403
            }

            mailboxRepository.putMailbox(mailbox.copy(state = state))

            val bridgeRepository = bridgeRepository.await()
            val bridges = bridgeRepository.queryBridge(emailAddress)

            val destinations = bridges.map { it.destination }

            val outboxRepository = outboxRepository.await()
            history.messagesAdded
                .flatMap {
                    destinations.map { destination ->
                        val outbox =
                            Outbox(
                                destination = destination,
                                sourceAddress = emailAddress,
                                messageId = it.message.id,
                            )
                        launch { outboxRepository.putOutbox(outbox) }
                    }
                }
                .joinAll()

            return@coroutineScope 201
        }
}
