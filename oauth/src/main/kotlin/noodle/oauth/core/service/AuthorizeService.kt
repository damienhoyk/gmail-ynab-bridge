package noodle.oauth.core.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import noodle.oauth.core.domain.AuthorizeCommand
import noodle.oauth.core.domain.Login
import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.domain.User
import noodle.oauth.core.port.LoginIdProvider
import noodle.oauth.core.port.LoginRepository
import noodle.oauth.core.port.OAuth2TokenProvider
import noodle.oauth.core.port.TokenRepository
import noodle.oauth.core.port.UserRepository
import org.slf4j.LoggerFactory

public class AuthorizeService(
    public val clientId: String,
    public val clientSecret: String,
    public val redirectUri: String,
    public val authClient: suspend () -> OAuth2TokenProvider,
    public val loginIdProvider: suspend () -> LoginIdProvider,
    public val tokenRepository: suspend () -> TokenRepository,
    public val userRepository: suspend () -> UserRepository,
    public val loginRepository: suspend () -> LoginRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    public fun execute(command: AuthorizeCommand): Int =
        runBlocking {
            val code = command.code
            val state = command.state

            if (code == null) {
                log.error("💩 code is null")
                return@runBlocking 400
            }

            if (state == null) {
                log.error("💩 state is null")
                return@runBlocking 400
            }

            val request =
                OAuth2TokenRequest(
                    code,
                    clientId,
                    clientSecret,
                    redirectUri,
                )

            val authClient = authClient()
            val oAuthToken = authClient.getToken(request)

            val loginIdProvider = loginIdProvider()
            val loginId = loginIdProvider.getLoginId(oAuthToken)

            if (loginId.isNullOrBlank()) {
                log.error("🐳 loginId is null")
                return@runBlocking 500
            }

            val tokenRepository = tokenRepository()
            val userId = tokenRepository.getUserId(state)

            if (userId.isNullOrEmpty()) {
                log.error("🐳 user id is null")
                return@runBlocking 500
            }

            log.info("🪪 Updating user login mapping for [{}] ...", userId)

            val loginRepository = loginRepository()
            loginRepository.putLogin(Login(loginId, userId))

            val userRepository = userRepository()
            userRepository.putUser(User(userId, loginId))

            log.info("🎫 Storing tokens for authorization [{}] ...", loginId)

            coroutineScope {
                launch {
                    oAuthToken.accessToken?.let { tokenRepository.updateTokenValue(loginId, "access", it) }
                }
                launch {
                    oAuthToken.refreshToken?.let { tokenRepository.updateTokenValue(loginId, "refresh", it) }
                }
            }

            return@runBlocking 200
        }
}
