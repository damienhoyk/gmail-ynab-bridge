package noodle.oauth.infrastructure.api.google

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import noodle.google.auth.infrastructure.api.KtorGoogleAuthClient
import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.domain.TokenResponse
import noodle.oauth.core.port.GoogleAuthClient
import noodle.oauth.core.port.LoginIdProvider
import noodle.oauth.core.port.OAuth2TokenProvider
import noodle.oauth.infrastructure.api.KtorOidcClient
import noodle.oauth.infrastructure.serialization.TokenInfoResponse

class KtorGoogleAuthClientAdapter(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) : KtorOidcClient(
        httpClient,
        "https://accounts.google.com/.well-known/openid-configuration",
        block,
    ),
    OAuth2TokenProvider,
    GoogleAuthClient,
    LoginIdProvider {
    private val googleAuthClient = KtorGoogleAuthClient(httpClient, block)

    override suspend fun getToken(request: OAuth2TokenRequest) = super.getToken(request)

    override suspend fun getTokenInfo(token: String) =
        googleAuthClient
            .getTokenInfo {
                setBody(FormDataContent(Parameters.build { append("id_token", token) }))
            }.body<TokenInfoResponse>()
            .domain()

    override suspend fun getLoginId(response: TokenResponse) = response.idToken?.let { getTokenInfo(it).email }
}
