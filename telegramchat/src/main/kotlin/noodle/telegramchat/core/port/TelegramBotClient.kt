package noodle.telegramchat.core.port

interface TelegramBotClient {
    suspend fun sendChatAction(
        chatId: String,
        action: String,
    )

    suspend fun sendMessage(
        chatId: String,
        message: String,
    )
}
