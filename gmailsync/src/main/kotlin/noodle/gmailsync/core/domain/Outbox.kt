package noodle.gmailsync.core.domain

public data class Outbox(
    public val destination: String,
    public val source: String,
) {
    public constructor(
        destination: String,
        sourceAddress: String,
        messageId: String?,
    ) : this(
        destination,
        "//$sourceAddress/messageId/$messageId",
    )
}
