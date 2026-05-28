package noodle.oauth.infrastructure.api.ynab

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import kotlinx.coroutines.Deferred
import noodle.oauth.core.port.YnabAuthClient
import noodle.oauth.infrastructure.api.KtorOAuth2TokenProvider
import noodle.ynab.auth.infrastructure.api.YnabAuthApi

class KtorYnabAuthClient(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) : KtorOAuth2TokenProvider(),
    YnabAuthClient {
    private val ynabAuthApi = YnabAuthApi(httpClient, block)

    override val httpClient: HttpClient
        get() = ynabAuthApi.httpClient

    override val tokenEndpoint: Deferred<String>
        get() = ynabAuthApi.tokenEndpoint
}
