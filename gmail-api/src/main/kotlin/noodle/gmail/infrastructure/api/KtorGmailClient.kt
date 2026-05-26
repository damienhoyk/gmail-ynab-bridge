package noodle.gmail.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

open class KtorGmailClient(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val httpClient =
        httpClient.config {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }

            defaultRequest {
                contentType(ContentType.Application.Json)
                url("https://gmail.googleapis.com/gmail/v1/users/")
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }

            block()
        }

    suspend fun getHistory(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.get("$user/history", block)

    suspend fun getMessage(
        user: String = "me",
        messageId: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.get("$user/messages/$messageId", block)

    suspend fun getProfile(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit,
    ) = httpClient.get("$user/profile", block)

    suspend fun getLabels(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.get("$user/labels", block)

    suspend fun postWatch(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.post("$user/watch", block)

    suspend fun postStop(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.post("$user/stop", block)
}
