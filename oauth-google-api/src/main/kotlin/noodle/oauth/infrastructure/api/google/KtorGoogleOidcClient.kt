package noodle.oauth.infrastructure.api.google

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.port.OAuth2TokenProvider
import noodle.oauth.infrastructure.api.KtorOidcClient

class KtorGoogleOidcClient(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) : KtorOidcClient(
        httpClient,
        "https://accounts.google.com/.well-known/openid-configuration",
        block,
    ),
    OAuth2TokenProvider {
    override suspend fun getToken(request: OAuth2TokenRequest) = super.getToken(request)
}
