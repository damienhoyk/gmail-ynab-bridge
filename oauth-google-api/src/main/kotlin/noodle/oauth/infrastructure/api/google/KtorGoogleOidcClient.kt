package noodle.oauth.infrastructure.api.google

import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.port.OAuth2TokenProvider
import noodle.oauth.infrastructure.api.OidcApi

class KtorGoogleOidcClient(
    private val oidcApi: OidcApi,
) : OAuth2TokenProvider {
    override suspend fun getToken(request: OAuth2TokenRequest) = oidcApi.getToken(request)
}
