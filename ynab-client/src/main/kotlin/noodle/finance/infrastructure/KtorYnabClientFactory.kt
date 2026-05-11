package noodle.finance.infrastructure

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import noodle.finance.infrastructure.out.KtorYnabClientAdapter
import noodle.finance.port.out.YnabClientFactory
import noodle.security.infrastructure.bearer
import noodle.security.port.`in`.AuthTokenService

class KtorYnabClientFactory(
    private val service: AuthTokenService,
    private val engine: HttpClientEngine,
) : YnabClientFactory {
    override suspend fun create(loginId: String) =
        KtorYnabClientAdapter(HttpClient(engine)) {
            install(Auth) { bearer(service, loginId) }
        }
}
