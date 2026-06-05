package noodle.oauth.infrastructure.api

import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.domain.TokenResponse
import noodle.oauth.core.port.OAuth2TokenProvider
import noodle.oauth2.infrastructure.api.OAuth2TokenApi
import noodle.oauth2.infrastructure.api.OAuth2TokenResponse

public class OAuth2Client(
    private val oauth2TokenApi: OAuth2TokenApi,
) : OAuth2TokenProvider {
    public override suspend fun getToken(request: OAuth2TokenRequest): TokenResponse =
        oauth2TokenApi
            .postToken {
                setBody(FormDataContent(request.toForm()))
            }.body<OAuth2TokenResponse>()
            .domain()
}

internal fun OAuth2TokenResponse.domain(): noodle.oauth.core.domain.TokenResponse =
    noodle.oauth.core.domain.TokenResponse(
        accessToken = accessToken,
        idToken = idToken,
        refreshToken = refreshToken,
        expiresIn = expiresIn,
        error = error,
    )
