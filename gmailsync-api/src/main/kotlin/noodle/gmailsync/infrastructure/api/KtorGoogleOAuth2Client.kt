package noodle.gmailsync.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import noodle.gmailsync.core.domain.TokenInfoResponse
import noodle.gmailsync.core.port.OAuth2Client
import noodle.oauth.infrastructure.api.TokenInfoResponse as OAuthTokenInfoResponse

class KtorGoogleOAuth2Client(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) : noodle.google.auth.infrastructure.api.KtorGoogleOAuth2Client(httpClient, block),
    OAuth2Client {
    override suspend fun getTokenInfo(token: String): TokenInfoResponse {
        val tokenInfo =
            getTokenInfo {
                setBody(FormDataContent(Parameters.build { append("id_token", token) }))
            }.body<OAuthTokenInfoResponse>()
        return TokenInfoResponse(email = tokenInfo.email)
    }
}
