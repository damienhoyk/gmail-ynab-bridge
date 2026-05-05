package noodle.chat.port.`in`

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
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
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class TelegramBotService(
    private val botClient: suspend () -> TelegramBotClient,
    private val googleAuthorizationUrl: suspend () -> String,
    private val ynabAuthorizationUrl: suspend () -> String,
    private val topicName: String,
    private val labelName: String,
    private val mailboxRepository: suspend () -> MailboxRepository,
    private val loginRepository: suspend () -> LoginRepository,
    private val tokenRepository: suspend () -> TokenRepository,
    private val userRepository: suspend () -> UserRepository,
    private val gmailClientFactory: suspend () -> GmailClientFactory,
) {
    suspend fun execute(command: RespondChatCommand): Int =
        coroutineScope {
            val text = command.text ?: return@coroutineScope 400
            val chatId = command.chatId ?: return@coroutineScope 400
            val authority = command.authority ?: return@coroutineScope 400

            val botClient = botClient()

            if (text.equals("/start", true)) {
                val loginRepository = loginRepository()
                val userRepository = userRepository()

                botClient.sendChatAction(chatId, "typing")

                val login = loginRepository.getLogin(authority)
                val userId = login?.userId ?: UUID.randomUUID().toString()

                loginRepository.putLogin(Login(authority, userId))
                userRepository.putUser(User(userId, authority))
            }

            if (text.equals("/authorizegmail", true)) {
                val loginRepository = loginRepository()
                val tokenRepository = tokenRepository()
                val googleAuthorizationUrl = googleAuthorizationUrl()

                botClient.sendChatAction(chatId, "typing")
                val login = loginRepository.getLogin(authority)
                val userId = login?.userId
                val token = UUID.randomUUID().toString()

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

                val login = loginRepository.getLogin(authority)
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

                val login = loginRepository.getLogin(authority)
                val userId = login?.userId ?: return@coroutineScope 400
                val user = userRepository.queryUser(userId)

                val emails = user.map { it.loginId }.filter { it.endsWith("@gmail.com") }

                val mailboxRepository = mailboxRepository()

                supervisorScope {
                    emails.map { gmail ->
                        async {
                            val gmailClient = gmailClientFactory.create(gmail)
                            val state = gmailClient.getProfile()?.historyId
                            val labelId = gmailClient.getLabelId(labelName)
                            val labelIds = listOfNotNull(labelId)

                            mailboxRepository.updateMailbox(Mailbox(gmail, state))
                            gmailClient.postWatch(GmailWatchRequest(topicName, labelIds))
                            gmail
                        }
                    }
                        .forEach { job ->
                            runCatching { job.await() }
                                .onFailure {
                                    it.printStackTrace()
                                    botClient.sendMessage(chatId, "🐳 ${it.message}")
                                }
                                .onSuccess {
                                    botClient.sendMessage(chatId, "🔭 I am now watching ${it.replace(".", "\\.")} label *$labelName*")
                                }
                        }
                }
            }

            return@coroutineScope 200
        }
}
