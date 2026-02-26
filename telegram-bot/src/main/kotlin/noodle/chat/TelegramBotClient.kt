package noodle.chat

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post

class TelegramBotClient(engine: HttpClientEngine = CIO.create(), block: HttpClientConfig<*>.() -> Unit = {}) {

    private val httpClient = HttpClient(engine) {

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.BODY
        }

        block()
    }

    suspend fun getMe() = httpClient.get("getMe")

    suspend fun setWebhook(url: String, block: HttpRequestBuilder.() -> Unit = {}) = httpClient.get("setWebhook") {
        parameter("url", url)
        block()
    }

    suspend fun sendMessage(chatId: String, message: String, block: HttpRequestBuilder.() -> Unit = {}) =
        httpClient.post("sendMessage") {
            parameter("chat_id", chatId)
            parameter("text", message)
            block()
        }

    suspend fun sendChatAction(chatId: String, action: String, block: HttpRequestBuilder.() -> Unit = {}) =
        httpClient.post("sendChatAction") {
            parameter("chat_id", chatId)
            parameter("action", action)
            block()
        }

}