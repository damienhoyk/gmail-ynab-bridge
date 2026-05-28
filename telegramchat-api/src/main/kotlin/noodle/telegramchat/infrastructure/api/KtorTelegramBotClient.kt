package noodle.telegramchat.infrastructure.api

import noodle.telegram.infrastructure.api.TelegramBotApi
import noodle.telegramchat.core.port.TelegramBotClient

class KtorTelegramBotClient(
    private val telegramBotApi: TelegramBotApi,
) : TelegramBotClient {
    override suspend fun sendChatAction(
        chatId: String,
        action: String,
    ) = telegramBotApi.sendChatAction(chatId, action)

    override suspend fun sendMessage(
        chatId: String,
        message: String,
    ) = telegramBotApi.sendMessage(chatId, message)
}
