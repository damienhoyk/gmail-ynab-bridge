package noodle.email.infrastructure.serialization

import kotlinx.serialization.Serializable

@Serializable
data class GmailLabel(
    val id: String,
    val name: String,
) {
    @Serializable data class List(val labels: kotlin.collections.List<GmailLabel> = emptyList())
}
