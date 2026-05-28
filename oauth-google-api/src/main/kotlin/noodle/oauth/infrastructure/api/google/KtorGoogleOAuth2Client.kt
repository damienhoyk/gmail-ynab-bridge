package noodle.oauth.infrastructure.api.google

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import noodle.google.auth.infrastructure.api.KtorGoogleOAuth2Client
import noodle.oauth.core.domain.TokenResponse
import noodle.oauth.core.port.LoginIdProvider
import noodle.oauth.infrastructure.api.TokenInfoResponse

class KtorGoogleOAuth2Client(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) : KtorGoogleOAuth2Client(httpClient, block),
    LoginIdProvider {
    override suspend fun getLoginId(response: TokenResponse): String? =
        response.idToken?.let { idToken ->
            getTokenInfo {
                setBody(FormDataContent(Parameters.build { append("id_token", idToken) }))
            }.body<TokenInfoResponse>().email
        }
}
