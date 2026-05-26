package noodle.gmailsync.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import noodle.gmailsync.core.port.GmailClientFactory
import noodle.oauth.core.service.TokenService
import noodle.oauth.infrastructure.api.bearer

class KtorGmailClientFactory(
    private val service: TokenService,
    private val engine: HttpClientEngine,
) : GmailClientFactory {
    override suspend fun create(loginId: String) =
        KtorGmailClient(HttpClient(engine)) {
            install(Auth) { bearer(service, loginId) }
        }
}
