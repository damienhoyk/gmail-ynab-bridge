package noodle.chat.domain

data class GmailLabel(val id: String, val name: String) {
    data class List(val labels: kotlin.collections.List<GmailLabel>? = null)
}
