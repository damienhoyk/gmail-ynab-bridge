package noodle.gmailsync.infrastructure.api

import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import noodle.gmailsync.core.domain.TokenInfoResponse
import noodle.gmailsync.core.port.GoogleTokenClient
import noodle.google.auth.infrastructure.api.KtorGoogleAuthClient
import noodle.oauth.infrastructure.serialization.TokenInfoResponse as OAuthTokenInfoResponse

class KtorGoogleAuthClient(
    private val googleAuthClient: KtorGoogleAuthClient,
) : GoogleTokenClient {
    override suspend fun getTokenInfo(token: String): TokenInfoResponse {
        val tokenInfo =
            googleAuthClient
                .getTokenInfo {
                    setBody(FormDataContent(Parameters.build { append("id_token", token) }))
                }.body<OAuthTokenInfoResponse>()
        return TokenInfoResponse(email = tokenInfo.email)
    }
}
