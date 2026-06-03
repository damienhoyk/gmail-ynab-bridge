package noodle.telegramchat.core.port

public interface TelegramBotClient {
    public suspend fun sendChatAction(
        chatId: String,
        action: String,
    )

    public suspend fun sendMessage(
        chatId: String,
        message: String,
    )
}
