package noodle.gmailsync.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthConfig
import noodle.gmail.infrastructure.api.GmailApi
import noodle.gmailsync.core.port.GmailClientFactory

class KtorGmailClientFactory(
    private val installAuth: AuthConfig.(loginId: String) -> Unit,
    private val engine: HttpClientEngine,
) : GmailClientFactory {
    override suspend fun create(loginId: String): noodle.gmailsync.core.port.GmailClient =
        KtorGmailClient(
            GmailApi(HttpClient(engine)) {
                install(Auth) { installAuth(loginId) }
            },
        )
}
