package noodle.chat.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import noodle.chat.core.port.GmailClientFactory
import noodle.security.core.service.AuthTokenService
import noodle.security.infrastructure.bearer

class KtorGmailClientFactory(
    private val service: AuthTokenService,
    private val engine: HttpClientEngine,
) : GmailClientFactory {
    override suspend fun create(loginId: String) =
        KtorGmailClientAdapter(HttpClient(engine)) {
            install(Auth) { bearer(service, loginId) }
        }
}
