package noodle.oauth.infrastructure.api.google

import io.ktor.client.call.body
import noodle.google.auth.infrastructure.api.GoogleOAuth2Api
import noodle.google.auth.infrastructure.api.model.TokenInfoResponse
import noodle.oauth.core.domain.TokenResponse
import noodle.oauth.core.port.LoginIdProvider

public class KtorGoogleLoginIdProvider(
    private val googleOAuth2Api: GoogleOAuth2Api,
) : LoginIdProvider {
    public override suspend fun getLoginId(response: TokenResponse): String? =
        response.idToken?.let { idToken ->
            googleOAuth2Api
                .requestTokenInfo { append("id_token", idToken) }
                .body<TokenInfoResponse>()
                .email
        }
}
