package noodle.oauth.infrastructure.api.google

import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.port.OAuth2TokenProvider
import noodle.oauth.infrastructure.api.OidcApi

public class KtorGoogleOidcClient(
    private val oidcApi: OidcApi,
) : OAuth2TokenProvider {
    public override suspend fun getToken(request: OAuth2TokenRequest): noodle.oauth.core.domain.TokenResponse = oidcApi.getToken(request)
}
