package noodle.telegram.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
public data class TelegramWebhookEvent(
    public val message: Message? = null,
) {
    @Serializable
    public data class Message(
        public val chat: Chat? = null,
        public val from: User? = null,
        public val text: String? = null,
    )

    @Serializable
    public data class Chat(
        public val id: Long? = null,
    )

    @Serializable
    public data class User(
        public val id: Long? = null,
    )
}
