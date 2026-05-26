package noodle.oauth.infrastructure.api.ynab

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.domain.TokenResponse
import noodle.oauth.core.port.OAuth2TokenProvider
import noodle.oauth.core.port.YnabAuthClient
import noodle.ynab.auth.infrastructure.api.KtorYnabAuthClient
import noodle.oauth.infrastructure.serialization.TokenResponse as SerializedTokenResponse

class KtorYnabAuthClientAdapter(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) : KtorYnabAuthClient(httpClient, block),
    OAuth2TokenProvider,
    YnabAuthClient {
    override suspend fun getToken(request: OAuth2TokenRequest): TokenResponse =
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
            }.body<SerializedTokenResponse>()
            .domain()
}
