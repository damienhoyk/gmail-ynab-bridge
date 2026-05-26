package noodle.telegram.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import noodle.telegramchat.core.port.TelegramBotClient

class KtorTelegramBotClient(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) : TelegramBotClient {
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

    suspend fun setWebhook(url: String) = setWebhook { parameter("url", url) }

    suspend fun sendMessage(
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.post("sendMessage", block)

    override suspend fun sendMessage(
        chatId: String,
        message: String,
    ) {
        sendMessage {
            parameter("chat_id", chatId)
            parameter("text", message)
            parameter("parse_mode", "MarkdownV2")
        }
    }

    suspend fun sendChatAction(
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.post("sendChatAction", block)

    override suspend fun sendChatAction(
        chatId: String,
        action: String,
    ) {
        sendChatAction {
            parameter("chat_id", chatId)
            parameter("action", action)
        }
    }
}
