package noodle.gmail.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
public data class GmailProfile(
    public val emailAddress: String,
    public val historyId: Long,
)
