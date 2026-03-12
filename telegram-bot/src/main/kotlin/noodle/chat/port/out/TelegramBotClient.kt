package noodle.chat.port.out

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
