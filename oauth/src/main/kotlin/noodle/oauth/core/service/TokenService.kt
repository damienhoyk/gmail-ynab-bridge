package noodle.oauth.core.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import noodle.oauth.core.domain.LoadTokensCommand
import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.domain.RefreshTokensCommand
import noodle.oauth.core.port.OAuth2TokenProvider
import noodle.oauth.core.port.TokenRepository
import org.slf4j.LoggerFactory

public class TokenService(
    private val clientId: String,
    private val clientSecret: String,
    private val tokenRepository: TokenRepository,
    private val authClient: OAuth2TokenProvider,
    private val refreshTokenTtlSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    public suspend fun execute(command: LoadTokensCommand): String = tokenRepository.getAccessToken(command.loginId)!!

    public suspend fun execute(command: RefreshTokensCommand): String {
        val loginId = command.loginId
        val value = tokenRepository.getRefreshToken(loginId)!!

        val authRequest =
            OAuth2TokenRequest(
                grantType = "refresh_token",
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = value,
            )

        val oAuthToken = authClient.getToken(authRequest)

        coroutineScope {
            launch {
                oAuthToken.accessToken?.let { tokenRepository.updateTokenValue(loginId, "access", it, oAuthToken.expiresIn?.toLong()) }
            }
            launch {
                oAuthToken.refreshToken?.let { tokenRepository.updateTokenValue(loginId, "refresh", it, refreshTokenTtlSeconds) }
            }
        }

        if (oAuthToken.accessToken.isNullOrBlank()) {
            log.error("🐳 refresh token is invalid")
            throw IllegalStateException("$loginId refresh token is invalid")
        }

        return oAuthToken.accessToken
    }
}
