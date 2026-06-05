package noodle.gmailsync.infrastructure.api

import io.ktor.client.call.body
import noodle.gmailsync.core.port.LoginIdProvider
import noodle.google.auth.infrastructure.api.GoogleOAuth2Api
import noodle.google.auth.infrastructure.api.model.TokenInfoResponse

public class KtorGoogleLoginIdProvider(
    private val googleOAuth2Api: GoogleOAuth2Api,
) : LoginIdProvider {
    override suspend fun getTokenInfo(token: String): String? =
        googleOAuth2Api
            .requestTokenInfo { append("id_token", token) }
            .body<TokenInfoResponse>()
            .email
}
