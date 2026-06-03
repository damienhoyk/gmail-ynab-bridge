package noodle.ynabsync.core.domain

public data class GmailMessage(
    public val id: String? = null,
    public val text: String? = null,
    public val senderEmail: String? = null,
    public val status: Int? = null,
)
