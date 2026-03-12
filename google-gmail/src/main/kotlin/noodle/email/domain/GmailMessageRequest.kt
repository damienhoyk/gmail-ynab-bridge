package noodle.email.domain

data class GmailMessageRequest(
    val id: String,
    val format: Format,
) {
    enum class Format(val value: String) {
        MINIMAL("minimal"),
        FULL("full"),
        RAW("raw"),
        METADATA("metadata"),
    }
}
