package noodle.telegram.bot

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import noodle.home.security.AccessTokenProvider

class TelegramBotClient(accessTokenProvider: AccessTokenProvider) {

    val httpClient = HttpClient {

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.BODY
        }

        defaultRequest {
            val token = accessTokenProvider.getToken()

            contentType(ContentType.Application.Json)
            url("https://api.telegram.org/bot$token/")
        }

    }

    suspend fun getMe() = httpClient.get("getMe")

    suspend fun setWebhook(url: String, block: HttpRequestBuilder.() -> Unit = {}) = httpClient.get("setWebhook") {
        parameter("url", url)
        block()
    }

    suspend fun sendMessage(chatId: String, message: String, block: HttpRequestBuilder.() -> Unit = {}) = httpClient.post("sendMessage") {
        parameter("chat_id", chatId)
        parameter("text", message)
        block()
    }

    suspend fun sendChatAction(chatId: String, action: String, block: HttpRequestBuilder.() -> Unit = {}) = httpClient.post("sendChatAction") {
        parameter("chat_id", chatId)
        parameter("action", action)
        block()
    }

}