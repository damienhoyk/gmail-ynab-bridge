package noodle.chat.infrastructure.out

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import noodle.chat.core.port.GmailClientFactory
import noodle.security.infrastructure.bearer
import noodle.security.port.`in`.AuthTokenService

class KtorGmailClientFactory(
    private val service: AuthTokenService,
    private val engine: HttpClientEngine,
) : GmailClientFactory {
    override suspend fun create(loginId: String) =
        KtorGmailClientAdapter(HttpClient(engine)) {
            install(Auth) { bearer(service, loginId) }
        }
}
