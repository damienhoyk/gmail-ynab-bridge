package noodle.tokenrefresher.infrastructure.api.google

import io.ktor.client.call.body
import noodle.oauth2.infrastructure.api.OAuth2TokenResponse
import noodle.oauth2.infrastructure.api.OidcApi
import noodle.tokenrefresher.core.domain.TokenResponse
import noodle.tokenrefresher.core.port.OAuth2TokenProvider

public class KtorGoogleTokenRefresher(
    private val oidcApi: OidcApi,
    private val clientId: String,
    private val clientSecret: String,
) : OAuth2TokenProvider {
    override suspend fun refresh(refreshToken: String): TokenResponse =
        oidcApi
            .requestToken {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", clientId)
                append("client_secret", clientSecret)
            }!!
            .body<OAuth2TokenResponse>()
            .let { TokenResponse(it.accessToken, it.refreshToken, it.expiresIn?.toLong() ?: 0) }
}
