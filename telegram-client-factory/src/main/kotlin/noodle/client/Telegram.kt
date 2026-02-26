package noodle.client

import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.coroutineScope
import noodle.chat.TelegramBotClient

class Telegram(
    private val token: String
) {

    suspend fun botClient() = coroutineScope {
        TelegramBotClient {
            defaultRequest {
                contentType(ContentType.Application.Json)
                url("https://api.telegram.org/bot$token/")
            }
        }
    }

}