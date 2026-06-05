package noodle.oauth2.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import kotlinx.coroutines.runBlocking
import noodle.ktor.defaultJson
import noodle.ktor.defaultLogging

public class OidcApi(
    private val discoveryUrl: String,
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val httpClient: HttpClient =
        httpClient.config {
            defaultLogging()
            defaultJson()
            block()
        }

    private val discoveryDocument = runBlocking { getDiscoveryDocument().body<OidcDiscoveryDocument>() }

    public suspend fun getDiscoveryDocument(block: HttpRequestBuilder.() -> Unit = {}): HttpResponse = httpClient.get(discoveryUrl, block)

    public suspend fun requestToken(parameters: Parameters): HttpResponse? =
        discoveryDocument.tokenEndpoint?.let {
            httpClient.submitForm(it, parameters)
        }
}
