package noodle.security.core.service

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import noodle.security.core.domain.AuthorizeCommand
import noodle.security.core.domain.Login
import noodle.security.core.domain.OAuth2TokenRequest
import noodle.security.core.domain.User
import noodle.security.core.port.LoginIdProvider
import noodle.security.core.port.LoginRepository
import noodle.security.core.port.OAuth2TokenProvider
import noodle.security.core.port.TokenRepository
import noodle.security.core.port.UserRepository
import org.slf4j.LoggerFactory

class OAuth2Service(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val authClient: suspend () -> OAuth2TokenProvider,
    val loginIdProvider: suspend () -> LoginIdProvider,
    val tokenRepository: suspend () -> TokenRepository,
    val userRepository: suspend () -> UserRepository,
    val loginRepository: suspend () -> LoginRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(command: AuthorizeCommand) =
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
            val response = authClient.getToken(request)

            val loginIdProvider = loginIdProvider()
            val authority = loginIdProvider.getLoginId(response)

            if (authority.isNullOrBlank()) {
                log.error("🐳 authority is null")
                return@runBlocking 500
            }

            val tokenRepository = tokenRepository()
            val token = tokenRepository.getToken(state, "state")

            val userId = token?.value

            if (userId.isNullOrEmpty()) {
                log.error("🐳 user id is null")
                return@runBlocking 500
            }

            log.info("🪪 Updating user login mapping for [{}] ...", userId)

            val loginRepository = loginRepository()
            loginRepository.putLogin(Login(authority, userId))

            val userRepository = userRepository()
            userRepository.putUser(User(userId, authority))

            log.info("🎫 Storing tokens for authorization [{}] ...", authority)

            launch {
                tokenRepository.updateTokenValue(authority, "access", response.accessToken!!)
            }

            launch {
                tokenRepository.updateTokenValue(authority, "refresh", response.refreshToken!!)
            }

            return@runBlocking 200
        }
}
