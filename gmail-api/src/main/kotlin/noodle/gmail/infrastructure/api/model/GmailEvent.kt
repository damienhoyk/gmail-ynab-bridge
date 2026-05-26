package noodle.gmail.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
data class GmailEvent(
    val emailAddress: String,
    val historyId: Long,
)
