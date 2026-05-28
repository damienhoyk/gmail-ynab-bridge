package noodle.telegramchat.infrastructure.api

import io.ktor.client.request.parameter
import noodle.telegram.infrastructure.api.TelegramBotApi
import noodle.telegramchat.core.port.TelegramBotClient

class KtorTelegramBotClient(
    private val telegramBotApi: TelegramBotApi,
) : TelegramBotClient {
    override suspend fun sendChatAction(
        chatId: String,
        action: String,
    ) {
        telegramBotApi.sendChatAction {
            parameter("chat_id", chatId)
            parameter("action", action)
        }
    }

    override suspend fun sendMessage(
        chatId: String,
        message: String,
    ) {
        telegramBotApi.sendMessage {
            parameter("chat_id", chatId)
            parameter("text", message)
            parameter("parse_mode", "MarkdownV2")
        }
    }
}
