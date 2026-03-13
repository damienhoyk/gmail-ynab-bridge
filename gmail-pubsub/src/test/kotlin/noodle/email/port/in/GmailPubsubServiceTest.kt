package noodle.email.port.`in`

import kotlinx.coroutines.runBlocking
import noodle.email.domain.*
import noodle.email.port.out.*
import noodle.security.domain.TokenInfoResponse
import noodle.security.port.out.GoogleAuthClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GmailPubsubServiceTest {
    private val email = "test@example.com"
    private val state = 12345L
    private val nextState = 67890L

    private val savedMailboxes = mutableListOf<Mailbox>()
    private val savedOutboxes = mutableListOf<Outbox>()

    private val googleAuthClient =
        object : GoogleAuthClient {
            override suspend fun getToken(request: noodle.security.domain.OAuth2TokenRequest) = TODO()

            override suspend fun getTokenInfo(token: String) =
                when (token) {
                    "valid-token" -> TokenInfoResponse(email = email)
                    "forbidden-token" -> TokenInfoResponse(email = null)
                    else -> TokenInfoResponse(email = null)
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
            override suspend fun getHistory(request: GmailHistoryRequest) =
                when (request.startHistoryId) {
                    state ->
                        GmailHistory(
                            history =
                                listOf(
                                    GmailHistory.Change(
                                        messagesAdded =
                                            listOf(
                                                GmailHistory.Message(GmailMessage(id = "msg1")),
                                            ),
                                    ),
                                ),
                        )
                    else -> GmailHistory()
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
                    email -> listOf(Bridge(source, "dest1"))
                    else -> emptyList()
                }
        }

    private val outboxRepository =
        object : OutboxRepository {
            override suspend fun putOutbox(outbox: Outbox) {
                savedOutboxes.add(outbox)
            }
        }

    private val service =
        GmailPubsubService(
            gmailClientFactory = { gmailClientFactory },
            googleAuthClient = { googleAuthClient },
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
            assertEquals("dest1", savedOutboxes[0].destination)
            assertEquals("msg1", savedOutboxes[0].source.substringBefore(":"))
        }
}
