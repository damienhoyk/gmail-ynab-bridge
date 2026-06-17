package noodle.telegramchat.core.service

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import noodle.telegramchat.core.domain.Login
import noodle.telegramchat.core.domain.Mailbox
import noodle.telegramchat.core.domain.RespondChatCommand
import noodle.telegramchat.core.domain.StateToken
import noodle.telegramchat.core.domain.User
import noodle.telegramchat.core.domain.WatchMailboxRequest
import noodle.telegramchat.core.port.GmailClientFactory
import noodle.telegramchat.core.port.LoginRepository
import noodle.telegramchat.core.port.MailboxRepository
import noodle.telegramchat.core.port.TelegramBotClient
import noodle.telegramchat.core.port.TokenRepository
import noodle.telegramchat.core.port.UserRepository
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

public class TelegramBotService(
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
    private val log = LoggerFactory.getLogger(javaClass)

    public suspend fun execute(command: RespondChatCommand): Int =
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

                val googleLogins = user.map { it.loginId }.filter { it.endsWith("@google.com") }

                log.info("found [{}] Google logins for user", googleLogins.count())

                val mailboxRepository = mailboxRepository()

                supervisorScope {
                    googleLogins
                        .map {
                            async {
                                val gmailClient = gmailClientFactory.create(it)
                                val profile = gmailClient.getProfile() ?: error("no gmail profile for [$it]")
                                val labelId = gmailClient.getLabelId(labelName)
                                val labelIds = listOfNotNull(labelId)

                                mailboxRepository.updateMailbox(Mailbox(profile.emailAddress, profile.historyId))
                                gmailClient.postWatch(WatchMailboxRequest(topicName, labelIds))
                                profile.emailAddress
                            }.runCatching { await() }
                        }.map { result ->
                            result
                                .onFailure {
                                    it.printStackTrace()
                                    botClient.sendMessage(chatId, "🐳 ${it.message}")
                                }.onSuccess {
                                    botClient.sendMessage(chatId, "🔭 I am now watching ${it.replace(".", "\\.")} label *$labelName*")
                                }
                        }
                }
            }

            return@coroutineScope 200
        }
}
