package noodle.telegramchat.bootstrap

import kotlinx.coroutines.runBlocking
import noodle.telegramchat.core.domain.GmailProfile
import noodle.telegramchat.core.domain.GmailWatch
import noodle.telegramchat.core.domain.Login
import noodle.telegramchat.core.domain.Mailbox
import noodle.telegramchat.core.domain.RespondChatCommand
import noodle.telegramchat.core.domain.User
import noodle.telegramchat.core.domain.WatchMailboxRequest
import noodle.telegramchat.core.port.GmailClient
import noodle.telegramchat.core.port.GmailClientFactory
import noodle.telegramchat.core.port.LoginRepository
import noodle.telegramchat.core.port.MailboxRepository
import noodle.telegramchat.core.port.TelegramBotClient
import noodle.telegramchat.core.port.TokenRepository
import noodle.telegramchat.core.port.UserRepository
import noodle.telegramchat.core.service.TelegramBotService
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TelegramBotServiceTests {
    private val testTelegramUserId = "456"
    private val testChatId = "chat-123"
    private val testUserId = "user-id-789"
    private val testGoogleSub = "sub-google-abc"
    private val testGoogleSubHandle = "noodle.oauth://$testGoogleSub@google.com"
    private val testYnabHandle = "noodle.oauth://$testUserId@app.ynab.com"
    private val testRealGmailEmail = "real-user@gmail.com"
    private val testMailboxAddress = testRealGmailEmail
    private val testTopicName = "projects/test-project/topics/gmail-watch"
    private val testLabelName = "YNAB"

    @Test
    fun watchGmailUsesGoogleSubLogin(): Unit =
        runBlocking {
            // Arrange
            val fakeBotClient = FakeTelegramBotClient()
            val fakeLoginRepository = FakeLoginRepository(Login("$testTelegramUserId@web.telegram.org", testUserId))
            val fakeUserRepository =
                FakeUserRepository(
                    listOf(
                        User(testUserId, testGoogleSubHandle),
                        User(testUserId, testYnabHandle),
                    ),
                )
            val capturedGmailLoginIds = mutableListOf<String>()
            val fakeGmailClient = FakeGmailClient()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient, capturedGmailLoginIds)
            val capturedMailboxes = mutableListOf<Mailbox>()
            val fakeMailboxRepository = FakeMailboxRepository(capturedMailboxes)
            val fakeTokenRepository = FakeTokenRepository()

            val service =
                TelegramBotService(
                    botClient = { fakeBotClient },
                    googleAuthorizationUrl = { "https://google.auth.test" },
                    ynabAuthorizationUrl = { "https://ynab.auth.test" },
                    topicName = testTopicName,
                    labelName = testLabelName,
                    mailboxRepository = { fakeMailboxRepository },
                    loginRepository = { fakeLoginRepository },
                    tokenRepository = { fakeTokenRepository },
                    userRepository = { fakeUserRepository },
                    gmailClientFactory = { fakeGmailClientFactory },
                )

            val command =
                RespondChatCommand(
                    telegramUserId = testTelegramUserId,
                    text = "/watchgmail",
                    chatId = testChatId,
                )

            // Act
            val statusCode = service.execute(command)

            // Assert
            assertEquals(200, statusCode)
            // Gmail client is created with the full sub-handle
            assertEquals(listOf(testGoogleSubHandle), capturedGmailLoginIds)
            // Mailbox is written with the real email address from the Gmail profile
            assertEquals(1, capturedMailboxes.size)
            assertEquals(testRealGmailEmail, capturedMailboxes[0].address)
        }

    @Test
    fun watchGmailFailsWhenProfileIsNull(): Unit =
        runBlocking {
            // Arrange
            val fakeBotClient = FakeTelegramBotClient()
            val fakeLoginRepository = FakeLoginRepository(Login("$testTelegramUserId@web.telegram.org", testUserId))
            val fakeUserRepository =
                FakeUserRepository(
                    listOf(
                        User(testUserId, testGoogleSubHandle),
                    ),
                )
            val capturedGmailLoginIds = mutableListOf<String>()
            // Create a fake client that returns null profile
            val fakeGmailClient = FakeGmailClientNullProfile()
            val fakeGmailClientFactory = FakeGmailClientFactory(fakeGmailClient, capturedGmailLoginIds)
            val capturedMailboxes = mutableListOf<Mailbox>()
            val fakeMailboxRepository = FakeMailboxRepository(capturedMailboxes)
            val fakeTokenRepository = FakeTokenRepository()

            val service =
                TelegramBotService(
                    botClient = { fakeBotClient },
                    googleAuthorizationUrl = { "https://google.auth.test" },
                    ynabAuthorizationUrl = { "https://ynab.auth.test" },
                    topicName = testTopicName,
                    labelName = testLabelName,
                    mailboxRepository = { fakeMailboxRepository },
                    loginRepository = { fakeLoginRepository },
                    tokenRepository = { fakeTokenRepository },
                    userRepository = { fakeUserRepository },
                    gmailClientFactory = { fakeGmailClientFactory },
                )

            val command =
                RespondChatCommand(
                    telegramUserId = testTelegramUserId,
                    text = "/watchgmail",
                    chatId = testChatId,
                )

            // Act
            val statusCode = service.execute(command)

            // Assert
            assertEquals(200, statusCode)
            // Gmail client is created with the sub-handle
            assertEquals(listOf(testGoogleSubHandle), capturedGmailLoginIds)
            // But mailbox is NOT updated because profile was null
            assertEquals(0, capturedMailboxes.size)
        }

    private class FakeTelegramBotClient : TelegramBotClient {
        override suspend fun sendMessage(
            chatId: String,
            message: String,
        ) {}

        override suspend fun sendChatAction(
            chatId: String,
            action: String,
        ) {}
    }

    private class FakeLoginRepository(
        private val login: Login,
    ) : LoginRepository {
        override suspend fun getLogin(id: String): Login? = login

        override suspend fun putLogin(login: Login) {}
    }

    private class FakeUserRepository(
        private val users: List<User>,
    ) : UserRepository {
        override suspend fun queryUser(id: String): List<User> = users

        override suspend fun putUser(user: User) {}
    }

    private class FakeGmailClientFactory(
        private val client: GmailClient,
        private val capturedLoginIds: MutableList<String> = mutableListOf(),
    ) : GmailClientFactory {
        override suspend fun create(loginId: String): GmailClient {
            capturedLoginIds.add(loginId)
            return client
        }
    }

    private class FakeGmailClient : GmailClient {
        override suspend fun getProfile(): GmailProfile? = GmailProfile(emailAddress = "real-user@gmail.com", historyId = 456L)

        override suspend fun getLabelId(labelName: String): String? = "label-id-xyz"

        override suspend fun postWatch(request: WatchMailboxRequest): GmailWatch = GmailWatch(historyId = 456L, expiration = null, error = null)
    }

    private class FakeGmailClientNullProfile : GmailClient {
        override suspend fun getProfile(): GmailProfile? = null

        override suspend fun getLabelId(labelName: String): String? = "label-id-xyz"

        override suspend fun postWatch(request: WatchMailboxRequest): GmailWatch = GmailWatch(historyId = 456L, expiration = null, error = null)
    }

    private class FakeMailboxRepository(
        private val capturedMailboxes: MutableList<Mailbox>,
    ) : MailboxRepository {
        override suspend fun updateMailbox(mailbox: Mailbox) {
            capturedMailboxes.add(mailbox)
        }
    }

    private class FakeTokenRepository : TokenRepository {
        override suspend fun putToken(token: noodle.telegramchat.core.domain.StateToken) {}
    }
}
