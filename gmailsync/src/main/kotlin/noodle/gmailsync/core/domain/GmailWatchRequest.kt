package noodle.gmailsync.core.domain

data class GmailWatchRequest(
    val topicName: String? = null,
    val labelIds: List<String> = emptyList(),
    val labelFilterBehaviour: LabelFilterBehaviour = LabelFilterBehaviour.INCLUDE,
) {
    enum class LabelFilterBehaviour(val value: String) {
        INCLUDE("include"),
        EXCLUDE("exclude"),
    }
}
