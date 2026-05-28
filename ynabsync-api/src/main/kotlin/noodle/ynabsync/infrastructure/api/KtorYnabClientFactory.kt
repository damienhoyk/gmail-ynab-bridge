package noodle.ynabsync.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthConfig
import noodle.ynabsync.core.port.YnabClientFactory

class KtorYnabClientFactory(
    private val installAuth: AuthConfig.(loginId: String) -> Unit,
    private val engine: HttpClientEngine,
) : YnabClientFactory {
    override suspend fun create(loginId: String) =
        KtorYnabClientAdapter(HttpClient(engine)) {
            install(Auth) { installAuth(loginId) }
        }
}
