package noodle.ynab.auth.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import noodle.ktor.defaultJson
import noodle.ktor.defaultLogging

public class YnabAuthApi(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val initScope = CoroutineScope(Default)

    public val tokenEndpoint: Deferred<String> = initScope.async { "https://app.ynab.com/oauth/token" }

    public val httpClient: HttpClient =
        httpClient.config {
            defaultLogging()
            defaultJson()
            block()
        }
}
