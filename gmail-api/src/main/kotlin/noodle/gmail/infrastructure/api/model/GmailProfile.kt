package noodle.gmail.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
data class GmailProfile(
    val emailAddress: String,
    val historyId: Long,
)
