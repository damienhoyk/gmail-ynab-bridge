package noodle.gmail.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
public data class GmailHistory(
    public val history: List<Change> = emptyList(),
    public val historyId: Long? = null,
    public val nextPageToken: String? = null,
) {
    @Serializable
    public data class Change(
        public val messagesAdded: List<Message> = emptyList(),
    )

    @Serializable
    public data class Message(
        public val message: GmailMessage,
    )
}
