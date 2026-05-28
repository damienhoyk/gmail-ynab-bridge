package noodle.telegram.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders

class TelegramBotApi(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val httpClient =
        httpClient.config {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.BODY
                sanitizeHeader { it == HttpHeaders.Authorization }
            }

            block()
        }

    suspend fun getMe() = httpClient.get("getMe")

    suspend fun setWebhook(
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.get("setWebhook", block)

    suspend fun sendMessage(
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.post("sendMessage", block)

    suspend fun sendChatAction(
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.post("sendChatAction", block)
}
