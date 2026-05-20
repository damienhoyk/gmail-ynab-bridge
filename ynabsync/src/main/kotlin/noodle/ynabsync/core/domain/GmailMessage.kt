package noodle.ynabsync.core.domain

data class GmailMessage(
    val id: String? = null,
    val text: String? = null,
    val senderEmail: String? = null,
    val status: Int? = null,
)
