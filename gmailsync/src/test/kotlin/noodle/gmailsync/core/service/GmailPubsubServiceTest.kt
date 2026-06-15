package noodle.gmailsync.core.service

import kotlinx.coroutines.runBlocking
import noodle.gmailsync.core.domain.Bridge
import noodle.gmailsync.core.domain.Mailbox
import noodle.gmailsync.core.domain.Outbox
import noodle.gmailsync.core.domain.SyncMailboxCommand
import noodle.gmailsync.core.port.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GmailPubsubServiceTest {
    private val email = "test@example.com"
    private val state = 12345L
    private val nextState = 67890L

    private val savedMailboxes = mutableListOf<Mailbox>()
    private val savedOutboxes = mutableListOf<Outbox>()

    private val googleTokenClient =
        object : LoginIdProvider {
            override suspend fun getTokenInfo(token: String): String? =
                when (token) {
                    "valid-token" -> email
                    else -> null
                }
        }

    private val mailboxRepository =
        object : MailboxRepository {
            override suspend fun getMailbox(address: String) =
                when (address) {
                    "test@example.com" -> Mailbox(address, state)
                    else -> Mailbox(address, null)
                }

            override suspend fun putMailbox(mailbox: Mailbox) {
                savedMailboxes.add(mailbox)
            }
        }

    private val gmailClient =
        object : GmailClient {
            override suspend fun getAddedMessageIds(sinceHistoryId: Long) =
                when (sinceHistoryId) {
                    state -> listOf("msg1")
                    else -> emptyList()
                }
        }

    private val gmailClientFactory =
        object : GmailClientFactory {
            override suspend fun create(loginId: String) = gmailClient
        }

    private val bridgeRepository =
        object : BridgeRepository {
            override suspend fun queryBridge(source: String) =
                when (source) {
                    email ->
                        listOf(
                            Bridge(source, "urn:app.ynab.com:user-123"),
                            Bridge(source, "urn:app.ynab.com:user-123"),
                        )
                    else -> emptyList()
                }
        }

    private val outboxRepository =
        object : OutboxRepository {
            override suspend fun putOutbox(
                outbox: Outbox,
                duration: kotlin.time.Duration,
            ) {
                savedOutboxes.add(outbox)
            }
        }

    private val service =
        GmailPubsubService(
            gmailClientFactory = { gmailClientFactory },
            loginIdProvider = { googleTokenClient },
            bridgeRepository = { bridgeRepository },
            mailboxRepository = { mailboxRepository },
            outboxRepository = { outboxRepository },
        )

    @Test
    fun `should return 400 when bearer token is missing`() =
        runBlocking {
            val command = SyncMailboxCommand(email = email, authorization = "Bearer ", state = nextState)

            val result = service.execute(command)

            assertEquals(400, result)
        }

    @Test
    fun `should return 400 when email is missing`() =
        runBlocking {
            val command = SyncMailboxCommand(email = null, authorization = "Bearer valid-token", state = nextState)

            val result = service.execute(command)

            assertEquals(400, result)
        }

    @Test
    fun `should return 400 when state is missing`() =
        runBlocking {
            val command = SyncMailboxCommand(email = email, authorization = "Bearer valid-token", state = null)

            val result = service.execute(command)

            assertEquals(400, result)
        }

    @Test
    fun `should return 403 when authorization is missing`() =
        runBlocking {
            val command = SyncMailboxCommand(email = email, authorization = null, state = nextState)

            val result = service.execute(command)

            assertEquals(403, result)
        }

    @Test
    fun `should return 500 when mailbox state is null`() =
        runBlocking {
            val command = SyncMailboxCommand(email = "invalid@example.com", authorization = "Bearer valid-token", state = nextState)

            val result = service.execute(command)

            assertEquals(500, result)
        }

    @Test
    fun `should return 403 when token info has no email`() =
        runBlocking {
            val command = SyncMailboxCommand(email = email, authorization = "Bearer forbidden-token", state = nextState)

            val result = service.execute(command)

            assertEquals(403, result)
        }

    @Test
    fun `should return 201 and process messages on success`() =
        runBlocking {
            val command = SyncMailboxCommand(email = email, authorization = "Bearer valid-token", state = nextState)

            val result = service.execute(command)

            assertEquals(201, result)
            assertEquals(nextState, savedMailboxes.firstOrNull()?.state)
            assertEquals(1, savedOutboxes.size)
            assertEquals("urn:app.ynab.com:user-123", savedOutboxes[0].destination)
            assertEquals("msg1", savedOutboxes[0].source.substringBefore(":"))
        }
}
