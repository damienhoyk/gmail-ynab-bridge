package noodle.email.port.`in`

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

    suspend fun execute(command: SyncMailboxCommand) =
        coroutineScope {
            if (command.email.isNullOrEmpty()) return@coroutineScope 400
            if (command.state == null) return@coroutineScope 400

            log.info("📨 Got message from [{}]", command.email)

            if (command.authorization.isNullOrEmpty()) return@coroutineScope 403
            if (command.bearerToken.isEmpty()) return@coroutineScope 400

            val googleAuthClient = googleAuthClient()
            val tokenInfoAsync = async { googleAuthClient.getTokenInfo(command.bearerToken) }

            val mailboxRepository = mailboxRepository()
            val mailboxAsync = async { mailboxRepository.getMailbox(command.email) }

            val bridgeRepository = bridgeRepository()
            val destinationsAsync = async { bridgeRepository.queryBridge(command.email).map { it.destination } }

            val gmailClientFactory = gmailClientFactory()
            val googleGmailClient = gmailClientFactory.create(command.email)

            val mailbox = mailboxAsync.await()
            val mailboxState = mailbox.state

            if (mailboxState == null) {
                log.warn("invalid mailbox state")
                return@coroutineScope 500
            }

            val messageIdsAsync = async { googleGmailClient.getAddedMessageIds(mailboxState) }

            val tokenInfo = tokenInfoAsync.await()
            if (tokenInfo.email.isNullOrEmpty()) return@coroutineScope 403

            val messageIds = messageIdsAsync.await()

            val outboxRepository = outboxRepository()
            val destinations = destinationsAsync.await()

            launch { mailboxRepository.putMailbox(mailbox.copy(state = command.state)) }
            messageIds.forEach { messageId ->
                destinations.forEach { destination ->
                    launch { outboxRepository.putOutbox(Outbox(destination = destination, sourceAddress = command.email, messageId = messageId)) }
                }
            }

            201
        }
}
