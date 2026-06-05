package noodle.telegramchat.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthConfig
import noodle.gmail.infrastructure.api.GmailApi
import noodle.telegramchat.core.port.GmailClient
import noodle.telegramchat.core.port.GmailClientFactory

public class KtorGmailClientFactory(
    private val installAuth: AuthConfig.(loginId: String) -> Unit,
    private val engine: HttpClientEngine,
) : GmailClientFactory {
    override suspend fun create(loginId: String): GmailClient =
        KtorGmailClient(
            GmailApi(HttpClient(engine)) {
                install(Auth) { installAuth(loginId) }
            },
        )
}
