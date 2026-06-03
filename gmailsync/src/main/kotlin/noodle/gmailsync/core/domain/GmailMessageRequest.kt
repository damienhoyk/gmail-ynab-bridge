package noodle.gmailsync.core.domain

public data class GmailMessageRequest(
    public val id: String,
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
