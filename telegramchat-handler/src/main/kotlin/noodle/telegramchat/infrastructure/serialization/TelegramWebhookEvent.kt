package noodle.telegramchat.infrastructure.serialization

import kotlinx.serialization.Serializable

@Serializable
data class TelegramWebhookEvent(
    val message: Message? = null,
) {
    @Serializable
    data class Message(
        val chat: Chat? = null,
        val from: User? = null,
        val text: String? = null,
    )

    @Serializable
    data class Chat(
        val id: Long? = null,
    )

    @Serializable
    data class User(
        val id: Long? = null,
    )
}
