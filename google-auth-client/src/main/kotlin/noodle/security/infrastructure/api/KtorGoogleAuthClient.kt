package noodle.security.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import noodle.security.core.domain.OAuth2TokenRequest
import noodle.security.core.domain.TokenResponse
import noodle.security.core.port.GoogleAuthClient
import noodle.security.core.port.LoginIdProvider
import noodle.security.infrastructure.serialization.TokenInfoResponse

class KtorGoogleAuthClient(httpClient: HttpClient, block: HttpClientConfig<*>.() -> Unit = {}) :
    KtorOidcClient(
        httpClient,
        "https://accounts.google.com/.well-known/openid-configuration",
        block,
    ),
    GoogleAuthClient,
    LoginIdProvider {
    val tokenInfoEndpoint = "https://oauth2.googleapis.com/tokeninfo"

    override suspend fun getToken(request: OAuth2TokenRequest) = super.getToken(request)

    suspend fun getTokenInfo(block: HttpRequestBuilder.() -> Unit) =
        httpClient.post(tokenInfoEndpoint, block)

    override suspend fun getTokenInfo(token: String) =
        getTokenInfo {
            setBody(FormDataContent(Parameters.build { append("id_token", token) }))
        }
            .body<TokenInfoResponse>()
            .domain()

    override suspend fun getLoginId(response: TokenResponse) =
        response.idToken?.let { getTokenInfo(it).email }
}
