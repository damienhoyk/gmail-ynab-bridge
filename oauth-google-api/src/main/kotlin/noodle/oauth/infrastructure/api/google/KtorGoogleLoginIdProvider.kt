package noodle.oauth.infrastructure.api.google

import io.ktor.client.call.body
import noodle.google.auth.infrastructure.api.GoogleOAuth2Api
import noodle.google.auth.infrastructure.api.model.TokenInfoResponse
import noodle.oauth.core.domain.LoginIdentity
import noodle.oauth.core.domain.TokenResponse
import noodle.oauth.core.port.LoginIdProvider

public class KtorGoogleLoginIdProvider(
    private val googleOAuth2Api: GoogleOAuth2Api,
) : LoginIdProvider {
    public override suspend fun getLoginId(response: TokenResponse): LoginIdentity? =
        response.idToken?.let { idToken ->
            val tokenInfo =
                googleOAuth2Api
                    .requestTokenInfo { append("id_token", idToken) }
                    .body<TokenInfoResponse>()
            tokenInfo.sub?.let { sub ->
                LoginIdentity(
                    id = "//$sub@google.com",
                    aliases = listOfNotNull(tokenInfo.email?.let { "//$it" }),
                )
            }
        }
}
