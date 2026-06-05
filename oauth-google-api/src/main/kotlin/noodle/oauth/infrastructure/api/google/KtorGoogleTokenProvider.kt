package noodle.oauth.infrastructure.api.google

import io.ktor.client.call.body
import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.domain.TokenResponse
import noodle.oauth.core.port.OAuth2TokenProvider
import noodle.oauth2.infrastructure.api.OAuth2TokenResponse
import noodle.oauth2.infrastructure.api.OidcApi

public class KtorGoogleTokenProvider(
    private val oidcApi: OidcApi,
) : OAuth2TokenProvider {
    override suspend fun getToken(request: OAuth2TokenRequest): TokenResponse =
        oidcApi
            .requestToken {
                request.code?.let { append("code", it) }
                request.clientId?.let { append("client_id", it) }
                request.clientSecret?.let { append("client_secret", it) }
                request.redirectUri?.let { append("redirect_uri", it) }
                request.refreshToken?.let { append("refresh_token", it) }
                request.grantType?.let { append("grant_type", it) }
            }?.body<OAuth2TokenResponse>()
            ?.let {
                TokenResponse(
                    accessToken = it.accessToken,
                    idToken = it.idToken,
                    refreshToken = it.refreshToken,
                    expiresIn = it.expiresIn,
                    error = it.error,
                )
            }!!
}
