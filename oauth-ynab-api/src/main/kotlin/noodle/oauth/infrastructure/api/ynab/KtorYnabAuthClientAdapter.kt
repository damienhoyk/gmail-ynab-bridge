package noodle.oauth.infrastructure.api.ynab

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import kotlinx.coroutines.Deferred
import noodle.oauth.core.port.YnabAuthClient
import noodle.oauth.infrastructure.api.KtorOAuth2TokenProvider
import noodle.ynab.auth.infrastructure.api.KtorYnabAuthClient

class KtorYnabAuthClientAdapter(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) : KtorOAuth2TokenProvider(),
    YnabAuthClient {
    private val ynabClient = KtorYnabAuthClient(httpClient, block)

    override val httpClient: HttpClient
        get() = ynabClient.httpClient

    override val tokenEndpoint: Deferred<String>
        get() = ynabClient.tokenEndpoint
}
