package noodle.gmail.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
public data class GmailEvent(
    public val emailAddress: String,
    public val historyId: Long,
)
