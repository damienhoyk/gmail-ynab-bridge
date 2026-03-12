package noodle.finance.infrastructure

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import noodle.email.infrastructure.out.KtorGmailClient
import noodle.finance.port.out.GmailClientFactory
import noodle.security.infrastructure.bearer
import noodle.security.port.`in`.AuthTokenService

class KtorGmailClientFactory(
    private val service: AuthTokenService,
    private val engine: HttpClientEngine,
) : GmailClientFactory {
    override suspend fun create(loginId: String) =
        KtorGmailClient(HttpClient(engine)) {
            install(Auth) { bearer(service, loginId) }
        }
}
