package noodle.security.port.`in`

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import noodle.security.domain.LoadTokensCommand
import noodle.security.domain.OAuth2TokenRequest
import noodle.security.domain.RefreshTokensCommand
import noodle.security.port.out.OAuth2TokenProvider
import noodle.security.port.out.TokenRepository

class AuthTokenService(
    private val clientId: String,
    private val clientSecret: String,
    private val tokenRepository: TokenRepository,
    private val authClient: OAuth2TokenProvider,
) {
    suspend fun execute(command: LoadTokensCommand): String {
        val token = tokenRepository.getToken(command.loginId, "access")
        val value = token?.value!!
        return value
    }

    suspend fun execute(command: RefreshTokensCommand): String {
        val loginId = command.loginId
        val token = tokenRepository.getToken(loginId, "refresh")
        val value = token?.value!!

        val authRequest =
            OAuth2TokenRequest(
                grantType = "refresh_token",
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = value,
            )

        val response = authClient.getToken(authRequest)

        coroutineScope {
            launch {
                response.accessToken?.let { tokenRepository.updateTokenValue(loginId, "access", it) }
            }
            launch {
                response.refreshToken?.let { tokenRepository.updateTokenValue(loginId, "refresh", it) }
            }
        }

        return response.accessToken!!
    }
}
