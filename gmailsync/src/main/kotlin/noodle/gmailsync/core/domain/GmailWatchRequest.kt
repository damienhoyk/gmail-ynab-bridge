package noodle.gmailsync.core.domain

public data class GmailWatchRequest(
    public val topicName: String? = null,
    public val labelIds: List<String> = emptyList(),
    public val labelFilterBehaviour: LabelFilterBehaviour = LabelFilterBehaviour.INCLUDE,
) {
    public enum class LabelFilterBehaviour(
        public val value: String,
    ) {
        INCLUDE("include"),
        EXCLUDE("exclude"),
    }
}
