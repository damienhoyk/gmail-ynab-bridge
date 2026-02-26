package noodle.email

import kotlinx.serialization.Serializable

@Serializable
data class GmailProfile(
    val emailAddress: String,
    val historyId: Long
)