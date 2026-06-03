package noodle.gmail.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
public data class GmailLabel(
    public val id: String,
    public val name: String,
) {
    @Serializable
    public data class List(
        public val labels: kotlin.collections.List<GmailLabel> = emptyList(),
    ) {
        public fun toMap(): Map<String, String> = labels.associate { it.id to it.name }
    }
}
