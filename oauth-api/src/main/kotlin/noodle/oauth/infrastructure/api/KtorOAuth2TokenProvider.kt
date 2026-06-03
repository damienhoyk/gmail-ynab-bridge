package noodle.oauth.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import kotlinx.coroutines.Deferred
import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.port.OAuth2TokenProvider
import noodle.oauth.infrastructure.api.TokenResponse

public abstract class KtorOAuth2TokenProvider : OAuth2TokenProvider {
    public abstract val httpClient: HttpClient
    public abstract val tokenEndpoint: Deferred<String>

    public override suspend fun getToken(request: OAuth2TokenRequest): noodle.oauth.core.domain.TokenResponse =
        httpClient
            .post(tokenEndpoint.await()) {
                setBody(
                    FormDataContent(
                        Parameters.build {
                            request.code?.let { append("code", it) }
                            request.clientId?.let { append("client_id", it) }
                            request.clientSecret?.let { append("client_secret", it) }
                            request.redirectUri?.let { append("redirect_uri", it) }
                            request.refreshToken?.let { append("refresh_token", it) }
                            request.grantType?.let { append("grant_type", it) }
                        },
                    ),
                )
            }.body<TokenResponse>()
            .domain()
}
