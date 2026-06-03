package noodle.gmail.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
public data class GmailWatch(
    public val historyId: Long?,
    public val expiration: Long?,
    public val error: Error?,
) {
    @Serializable
    public data class Error(
        public val code: Int,
        public val message: String,
    )
}
