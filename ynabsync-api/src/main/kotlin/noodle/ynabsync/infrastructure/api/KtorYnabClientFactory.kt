package noodle.ynabsync.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthConfig
import noodle.ynab.infrastructure.api.YnabApi
import noodle.ynabsync.core.port.YnabClient
import noodle.ynabsync.core.port.YnabClientFactory

public class KtorYnabClientFactory(
    private val installAuth: AuthConfig.(loginId: String) -> Unit,
    private val engine: HttpClientEngine,
) : YnabClientFactory {
    override suspend fun create(loginId: String): YnabClient =
        KtorYnabClient(
            YnabApi(HttpClient(engine)) {
                install(Auth) { installAuth(loginId) }
            },
        )
}
