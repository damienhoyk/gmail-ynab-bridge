package noodle.oauth.infrastructure.api

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.port.OAuth2TokenProvider

public class OAuth2TokenClient(
    private val post: suspend (Parameters) -> HttpResponse,
) : OAuth2TokenProvider {
    public override suspend fun getToken(request: OAuth2TokenRequest): noodle.oauth.core.domain.TokenResponse = post(request.toForm()).body<TokenResponse>().domain()
}

internal fun OAuth2TokenRequest.toForm(): Parameters =
    Parameters.build {
        code?.let { append("code", it) }
        clientId?.let { append("client_id", it) }
        clientSecret?.let { append("client_secret", it) }
        redirectUri?.let { append("redirect_uri", it) }
        refreshToken?.let { append("refresh_token", it) }
        grantType?.let { append("grant_type", it) }
    }
