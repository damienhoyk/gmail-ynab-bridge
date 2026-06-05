package noodle.oauth2.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import noodle.ktor.defaultJson
import noodle.ktor.defaultLogging

public class OidcDiscoveryApi(
    httpClient: HttpClient,
    private val discoveryUrl: String,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val httpClient: HttpClient =
        httpClient.config {
            defaultLogging()
            defaultJson()
            block()
        }

    public suspend fun getDiscoveryDocument(block: HttpRequestBuilder.() -> Unit = {}): OidcDiscoveryDocument = httpClient.get(discoveryUrl, block).body()
}
