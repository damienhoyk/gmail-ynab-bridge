package noodle.telegram.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import noodle.ktor.defaultLogging

public class TelegramBotApi(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val httpClient =
        httpClient.config {
            defaultLogging(LogLevel.BODY)

            block()
        }

    public suspend fun getMe(): HttpResponse = httpClient.get("getMe")

    public suspend fun setWebhook(
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = httpClient.get("setWebhook", block)

    public suspend fun sendMessage(
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = httpClient.post("sendMessage", block)

    public suspend fun sendChatAction(
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = httpClient.post("sendChatAction", block)
}
