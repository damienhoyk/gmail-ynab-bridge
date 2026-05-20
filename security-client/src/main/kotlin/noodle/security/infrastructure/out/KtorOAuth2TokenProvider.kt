package noodle.security.infrastructure.out

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import kotlinx.coroutines.Deferred
import noodle.security.core.domain.OAuth2TokenRequest
import noodle.security.infrastructure.serialization.TokenResponse
import noodle.security.core.port.OAuth2TokenProvider

abstract class KtorOAuth2TokenProvider : OAuth2TokenProvider {
    abstract val httpClient: HttpClient
    abstract val tokenEndpoint: Deferred<String>

    override suspend fun getToken(request: OAuth2TokenRequest) =
        httpClient
            .post(tokenEndpoint.await()) {
                setBody(
                    FormDataContent(
                        Parameters.Companion.build {
                            request.code?.let { append("code", it) }
                            request.clientId?.let { append("client_id", it) }
                            request.clientSecret?.let { append("client_secret", it) }
                            request.redirectUri?.let { append("redirect_uri", it) }
                            request.refreshToken?.let { append("refresh_token", it) }
                            request.grantType?.let { append("grant_type", it) }
                        },
                    ),
                )
            }.body<TokenResponse>().domain()
}
