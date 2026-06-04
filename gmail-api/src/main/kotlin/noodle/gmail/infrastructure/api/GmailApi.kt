package noodle.gmail.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import noodle.ktor.defaultJson
import noodle.ktor.defaultLogging

public class GmailApi(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val httpClient =
        httpClient.config {
            defaultLogging()
            defaultJson(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                },
            )

            defaultRequest {
                contentType(ContentType.Application.Json)
                url("https://gmail.googleapis.com/gmail/v1/users/")
            }

            block()
        }

    public suspend fun getHistory(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = httpClient.get("$user/history", block)

    public suspend fun getMessage(
        user: String = "me",
        messageId: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = httpClient.get("$user/messages/$messageId", block)

    public suspend fun getProfile(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit,
    ): HttpResponse = httpClient.get("$user/profile", block)

    public suspend fun getLabels(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = httpClient.get("$user/labels", block)

    public suspend fun postWatch(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = httpClient.post("$user/watch", block)

    public suspend fun postStop(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = httpClient.post("$user/stop", block)
}
