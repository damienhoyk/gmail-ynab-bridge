package noodle.gmailsync.core.service

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import noodle.gmailsync.core.domain.Outbox
import noodle.gmailsync.core.domain.SyncMailboxCommand
import noodle.gmailsync.core.port.BridgeRepository
import noodle.gmailsync.core.port.GmailClientFactory
import noodle.gmailsync.core.port.LoginRepository
import noodle.gmailsync.core.port.MailboxRepository
import noodle.gmailsync.core.port.OutboxRepository
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.days

public class GmailPubsubService(
    private val gmailClientFactory: suspend () -> GmailClientFactory,
    private val loginRepository: suspend () -> LoginRepository,
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

            val loginRepository = loginRepository()
            val loginId = loginRepository.resolve(command.email) ?: return@coroutineScope 403

            val mailboxRepository = mailboxRepository()
            val mailboxAsync = async { mailboxRepository.getMailbox(command.email) }

            val bridgeRepository = bridgeRepository()
            val destinationsAsync = async { bridgeRepository.queryBridge(command.email).map { it.destination }.distinct() }

            val gmailClientFactory = gmailClientFactory()
            val googleGmailClient = gmailClientFactory.create(loginId)

            val mailbox = mailboxAsync.await()
            val mailboxState = mailbox.state

            if (mailboxState == null) {
                log.warn("invalid mailbox state")
                return@coroutineScope 500
            }

            val messageIdsAsync = async { googleGmailClient.getAddedMessageIds(mailboxState) }

            val messageIds = messageIdsAsync.await()

            val outboxRepository = outboxRepository()
            val destinations = destinationsAsync.await()

            launch { mailboxRepository.putMailbox(mailbox.copy(state = command.state)) }
            messageIds.forEach { messageId ->
                destinations.forEach { destination ->
                    launch { outboxRepository.putOutbox(Outbox(destination = destination, sourceAddress = command.email, messageId = messageId), 30.days) }
                }
            }

            201
        }
}
