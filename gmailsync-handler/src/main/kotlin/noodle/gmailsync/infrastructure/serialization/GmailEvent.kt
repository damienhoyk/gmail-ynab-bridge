package noodle.gmailsync.infrastructure.serialization

import kotlinx.serialization.Serializable

@Serializable
data class GmailEvent(
    val emailAddress: String,
    val historyId: Long,
)
