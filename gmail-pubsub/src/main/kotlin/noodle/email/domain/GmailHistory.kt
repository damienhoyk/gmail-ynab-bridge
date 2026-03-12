package noodle.email.domain

data class GmailHistory(
    val history: List<Change> = emptyList(),
    val historyId: Long? = null,
    val nextPageToken: String? = null,
) {
    val messagesAdded
        get() = history.flatMap { it.messagesAdded }

    data class Change(
        val messagesAdded: List<Message>,
    )

    data class Message(
        val message: GmailMessage,
    )
}
