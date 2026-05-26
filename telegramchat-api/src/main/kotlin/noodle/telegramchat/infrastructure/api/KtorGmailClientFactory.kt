package noodle.telegramchat.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import noodle.oauth.core.service.TokenService
import noodle.oauth.infrastructure.api.bearer
import noodle.telegramchat.core.port.GmailClientFactory

class KtorGmailClientFactory(
    private val service: TokenService,
    private val engine: HttpClientEngine,
) : GmailClientFactory {
    override suspend fun create(loginId: String) =
        KtorGmailClientAdapter(HttpClient(engine)) {
            install(Auth) { bearer(service, loginId) }
        }
}
