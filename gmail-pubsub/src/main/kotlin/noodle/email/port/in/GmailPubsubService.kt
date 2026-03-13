package noodle.email.port.`in`

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
    private val gmailClientFactory: suspend () -> GmailClientFactory,
    private val googleAuthClient: suspend () -> GoogleAuthClient,
    private val bridgeRepository: suspend () -> BridgeRepository,
    private val mailboxRepository: suspend () -> MailboxRepository,
    private val outboxRepository: suspend () -> OutboxRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val historyType = "messageAdded"

    suspend fun execute(command: SyncMailboxCommand) =
        coroutineScope {
            log.info("📨 Got message from [{}]", command.email)

            if (command.bearerToken.isEmpty()) {
                return@coroutineScope 400
            }

            val googleAuthClient = googleAuthClient()
            val tokenInfoAsync = async { googleAuthClient.getTokenInfo(command.bearerToken) }

            val mailboxRepository = mailboxRepository()
            val mailboxAsync = async { mailboxRepository.getMailbox(command.email) }

            val gmailClientFactory = gmailClientFactory()
            val googleGmailClient = gmailClientFactory.create(command.email)

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

            mailboxRepository.putMailbox(mailbox.copy(state = command.state))

            val bridgeRepository = bridgeRepository()
            val bridges = bridgeRepository.queryBridge(command.email)

            val destinations = bridges.map { it.destination }

            val outboxRepository = outboxRepository()
            history.messagesAdded
                .flatMap {
                    destinations.map { destination ->
                        val outbox =
                            Outbox(
                                destination = destination,
                                sourceAddress = command.email,
                                messageId = it.message.id,
                            )
                        launch { outboxRepository.putOutbox(outbox) }
                    }
                }
                .joinAll()

            return@coroutineScope 201
        }
}
