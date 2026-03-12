package noodle.email.infrastructure.serialization

import kotlinx.serialization.Serializable

@Serializable
data class GmailHistory(
    val history: List<Change> = emptyList(),
    val historyId: Long? = null,
    val nextPageToken: String? = null,
) {
    @Serializable
    data class Change(
        val messagesAdded: List<Message>,
    )

    @Serializable
    data class Message(
        val message: GmailMessage,
    )
}
