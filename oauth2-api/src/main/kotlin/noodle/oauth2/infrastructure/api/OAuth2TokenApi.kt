package noodle.oauth2.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import noodle.ktor.defaultJson
import noodle.ktor.defaultLogging

public class OAuth2TokenApi(
    httpClient: HttpClient,
    private val tokenEndpoint: String,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val httpClient: HttpClient =
        httpClient.config {
            defaultLogging()
            defaultJson()
            block()
        }

    public suspend fun postToken(block: HttpRequestBuilder.() -> Unit): HttpResponse = httpClient.post(tokenEndpoint, block)
}
