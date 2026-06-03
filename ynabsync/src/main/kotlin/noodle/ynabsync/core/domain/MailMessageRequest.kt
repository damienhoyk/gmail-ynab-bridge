package noodle.ynabsync.core.domain

public data class MailMessageRequest(
    public val messageId: String,
    public val format: Format,
) {
    public enum class Format(
        public val value: String,
    ) {
        MINIMAL("minimal"),
        FULL("full"),
        RAW("raw"),
        METADATA("metadata"),
    }
}
