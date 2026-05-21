package noodle.ynabsync.infrastructure

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import noodle.oauth.core.service.AuthTokenService
import noodle.oauth.infrastructure.bearer
import noodle.ynabsync.core.port.YnabClientFactory
import noodle.ynabsync.infrastructure.api.KtorYnabClientAdapter

class KtorYnabClientFactory(
    private val service: AuthTokenService,
    private val engine: HttpClientEngine,
) : YnabClientFactory {
    override suspend fun create(loginId: String) =
        KtorYnabClientAdapter(HttpClient(engine)) {
            install(Auth) { bearer(service, loginId) }
        }
}
