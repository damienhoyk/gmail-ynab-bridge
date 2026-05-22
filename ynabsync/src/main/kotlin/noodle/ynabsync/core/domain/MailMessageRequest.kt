package noodle.ynabsync.core.domain

data class MailMessageRequest(
    val messageId: String,
    val format: Format,
) {
    enum class Format(
        val value: String,
    ) {
        MINIMAL("minimal"),
        FULL("full"),
        RAW("raw"),
        METADATA("metadata"),
    }
}
