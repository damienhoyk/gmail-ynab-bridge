package noodle.gmailsync.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import noodle.gmailsync.core.port.GmailClientFactory
import noodle.oauth.core.service.AuthTokenService
import noodle.oauth.infrastructure.bearer

class KtorGmailClientFactory(
    private val service: AuthTokenService,
    private val engine: HttpClientEngine,
) : GmailClientFactory {
    override suspend fun create(loginId: String) =
        KtorGmailClientAdapter(HttpClient(engine)) {
            install(Auth) { bearer(service, loginId) }
        }
}
