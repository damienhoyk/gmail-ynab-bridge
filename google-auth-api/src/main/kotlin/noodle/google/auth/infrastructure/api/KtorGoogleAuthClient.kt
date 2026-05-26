package noodle.google.auth.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post

open class KtorGoogleAuthClient(
    protected val httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    val tokenInfoEndpoint = "https://oauth2.googleapis.com/tokeninfo"

    suspend fun getTokenInfo(block: HttpRequestBuilder.() -> Unit) = httpClient.post(tokenInfoEndpoint, block)

    // Raw accessor for post requests with custom body
    protected suspend fun postTokenInfo(block: HttpRequestBuilder.() -> Unit) = httpClient.post(tokenInfoEndpoint, block)
}
