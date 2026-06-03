package noodle.oauth.infrastructure.api.google

import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import noodle.google.auth.infrastructure.api.GoogleOAuth2Api
import noodle.oauth.core.domain.TokenResponse
import noodle.oauth.core.port.LoginIdProvider
import noodle.oauth.infrastructure.api.TokenInfoResponse

class KtorGoogleLoginIdProvider(
    private val googleOAuth2Api: GoogleOAuth2Api,
) : LoginIdProvider {
    override suspend fun getLoginId(response: TokenResponse): String? =
        response.idToken?.let { idToken ->
            googleOAuth2Api
                .getTokenInfo {
                    setBody(FormDataContent(Parameters.build { append("id_token", idToken) }))
                }.body<TokenInfoResponse>()
                .email
        }
}
