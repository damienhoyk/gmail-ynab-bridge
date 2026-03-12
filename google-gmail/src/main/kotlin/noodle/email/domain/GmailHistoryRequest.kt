package noodle.email.domain

data class GmailHistoryRequest(
    val startHistoryId: Long,
    val historyTypes: List<String> = emptyList(),
    val labelId: String? = null,
)
