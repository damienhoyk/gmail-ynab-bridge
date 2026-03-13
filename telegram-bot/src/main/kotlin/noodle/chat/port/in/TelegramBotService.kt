package noodle.chat.port.`in`

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import noodle.chat.domain.Login
import noodle.chat.domain.Mailbox
import noodle.chat.domain.RespondChatCommand
import noodle.chat.domain.StateToken
import noodle.chat.domain.User
import noodle.chat.port.out.GmailClientFactory
import noodle.chat.port.out.LoginRepository
import noodle.chat.port.out.MailboxRepository
import noodle.chat.port.out.TelegramBotClient
import noodle.chat.port.out.TokenRepository
import noodle.chat.port.out.UserRepository
import noodle.email.domain.GmailWatchRequest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class TelegramBotService(
    private val botClient: suspend () -> TelegramBotClient,
    private val googleAuthorizationUrl: suspend () -> String,
    private val ynabAuthorizationUrl: suspend () -> String,
    private val mailboxRepository: suspend () -> MailboxRepository,
    private val loginRepository: suspend () -> LoginRepository,
    private val tokenRepository: suspend () -> TokenRepository,
    private val userRepository: suspend () -> UserRepository,
    private val gmailClientFactory: suspend () -> GmailClientFactory,
) {
    suspend fun execute(command: RespondChatCommand) =
        coroutineScope {
            val text = command.message
            val chatId = command.chatId
            val botClient = botClient()

            if (text.equals("/start", true)) {
                val loginRepository = loginRepository()
                val userRepository = userRepository()

                botClient.sendChatAction(chatId, "typing")

                val login = loginRepository.getLogin(command.telegramUserId)
                val userId = login?.userId ?: UUID.randomUUID().toString()

                loginRepository.putLogin(Login(command.telegramUserId, userId))
                userRepository.putUser(User(userId, command.telegramUserId))
            }

            if (text.equals("/authorizegmail", true)) {
                val loginRepository = loginRepository()
                val tokenRepository = tokenRepository()
                val googleAuthorizationUrl = googleAuthorizationUrl()

                botClient.sendChatAction(chatId, "typing")
                val login = loginRepository.getLogin(command.telegramUserId)
                val userId = login?.userId
                val token = UUID.randomUUID().toString()

                val ttlInstant = Instant.now().plus(30, ChronoUnit.MINUTES)
                val ttl = ttlInstant.epochSecond

                if (userId.isNullOrEmpty()) {
                    return@coroutineScope 500
                }

                tokenRepository.putToken(StateToken(token, userId, 30.minutes))

                val message = "[🔑 Authorize Gmail]($googleAuthorizationUrl&state=$token)"
                botClient.sendMessage(chatId, message)
            }

            if (text.equals("/authorizeynab", true)) {
                botClient.sendChatAction(chatId, "typing")
                val loginRepository = loginRepository()
                val tokenRepository = tokenRepository()
                val ynabAuthorizationUrl = ynabAuthorizationUrl()

                val login = loginRepository.getLogin(command.telegramUserId)
                val userId = login?.userId
                val token = UUID.randomUUID().toString()

                tokenRepository.putToken(StateToken(token, userId, 30.minutes))

                val message = "[🔑 Authorize YNAB]($ynabAuthorizationUrl&state=$token)"
                botClient.sendMessage(chatId, message)
            }

            if (text.equals("/watchgmail", true)) {
                botClient.sendChatAction(chatId, "typing")
                val loginRepository = loginRepository()
                val userRepository = userRepository()
                val gmailClientFactory = gmailClientFactory()

                val login = loginRepository.getLogin(command.telegramUserId)
                val userId = login?.userId!!
                val user = userRepository.queryUser(userId)

                val emails = user.map { it.loginId }.filter { it.endsWith("@gmail.com") }

                val topicName = "projects/lexical-cider-458409-d5/topics/gmail"
                val labelName = "money"

                val mailboxRepository = mailboxRepository()

                val jobs =
                    emails.map { gmail ->
                        launch {
                            val gmailClient = gmailClientFactory.create(gmail)

                            val labels = gmailClient.getLabels()?.labels ?: emptyList()
                            val profile = gmailClient.getProfile()
                            val state = profile?.historyId

                            val labelIds =
                                labels.filter { it.name.equals(labelName, true) }.map { it.id }

                            mailboxRepository.updateMailbox(Mailbox(gmail, state))
                            gmailClient.postWatch(GmailWatchRequest(topicName, labelIds))
                        }
                    }

                jobs.joinAll()

                botClient.sendMessage(chatId, "🔭 I am now watching your gmails labelled *$labelName*")
            }

            return@coroutineScope 200
        }
}
