package noodle.google.auth.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import noodle.ktor.defaultJson
import noodle.ktor.defaultLogging

public class GoogleOAuth2Api(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val httpClient =
        httpClient.config {
            defaultLogging()
            defaultJson()
            block()
        }

    public suspend fun getTokenInfo(
        block: HttpRequestBuilder.() -> Unit,
    ): HttpResponse = httpClient.post("https://oauth2.googleapis.com/tokeninfo", block)
}
