package noodle.gmailsync.infrastructure.api

import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import noodle.gmailsync.core.port.OAuth2Client
import noodle.google.auth.infrastructure.api.GoogleOAuth2Api
import noodle.google.auth.infrastructure.api.model.TokenInfoResponse

public class KtorGoogleOAuth2Client(
    private val googleOAuth2Api: GoogleOAuth2Api,
) : OAuth2Client {
    override suspend fun getTokenInfo(token: String): String? =
        googleOAuth2Api
            .getTokenInfo {
                setBody(FormDataContent(Parameters.build { append("id_token", token) }))
            }.body<TokenInfoResponse>()
            .email
}
