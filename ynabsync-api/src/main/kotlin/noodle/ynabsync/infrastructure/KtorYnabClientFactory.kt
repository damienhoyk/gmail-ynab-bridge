package noodle.ynabsync.infrastructure

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import noodle.oauth.core.service.TokenService
import noodle.oauth.infrastructure.api.bearer
import noodle.ynabsync.core.port.YnabClientFactory
import noodle.ynabsync.infrastructure.api.KtorYnabClientAdapter

class KtorYnabClientFactory(
    private val service: TokenService,
    private val engine: HttpClientEngine,
) : YnabClientFactory {
    override suspend fun create(loginId: String) =
        KtorYnabClientAdapter(HttpClient(engine)) {
            install(Auth) { bearer(service, loginId) }
        }
}
