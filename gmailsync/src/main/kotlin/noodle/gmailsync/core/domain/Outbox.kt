package noodle.gmailsync.core.domain

data class Outbox(
    val destination: String,
    val source: String,
) {
    constructor(
        destination: String,
        sourceAddress: String,
        messageId: String?,
    ) : this(
        destination,
        "$messageId:$sourceAddress",
    )

    val messageId: String
        get() = destination.substringBefore(":")

    val sourceAddress: String
        get() = source.substringAfter(":")
}
