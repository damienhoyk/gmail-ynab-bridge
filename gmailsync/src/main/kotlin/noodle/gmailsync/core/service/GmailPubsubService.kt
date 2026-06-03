package noodle.gmailsync.core.service

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import noodle.gmailsync.core.domain.Outbox
import noodle.gmailsync.core.domain.SyncMailboxCommand
import noodle.gmailsync.core.port.BridgeRepository
import noodle.gmailsync.core.port.GmailClientFactory
import noodle.gmailsync.core.port.MailboxRepository
import noodle.gmailsync.core.port.OAuth2Client
import noodle.gmailsync.core.port.OutboxRepository
import org.slf4j.LoggerFactory

public class GmailPubsubService(
    private val gmailClientFactory: suspend () -> GmailClientFactory,
    private val googleTokenClient: suspend () -> OAuth2Client,
    private val bridgeRepository: suspend () -> BridgeRepository,
    private val mailboxRepository: suspend () -> MailboxRepository,
    private val outboxRepository: suspend () -> OutboxRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    public suspend fun execute(command: SyncMailboxCommand): Int =
        coroutineScope {
            if (command.email.isNullOrEmpty()) return@coroutineScope 400
            if (command.state == null) return@coroutineScope 400

            log.info("📨 Got message from [{}]", command.email)

            if (command.authorization.isNullOrEmpty()) return@coroutineScope 403
            if (command.bearerToken.isEmpty()) return@coroutineScope 400

            val googleTokenClient = googleTokenClient()
            val tokenInfoAsync = async { googleTokenClient.getTokenInfo(command.bearerToken) }

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

            val tokenEmail = tokenInfoAsync.await()
            if (tokenEmail.isNullOrEmpty()) return@coroutineScope 403

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
