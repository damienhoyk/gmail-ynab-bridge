package noodle.gmailsync.infrastructure.serialization

import kotlinx.serialization.Serializable

@Serializable
data class GmailProfile(
    val emailAddress: String,
    val historyId: Long,
)
