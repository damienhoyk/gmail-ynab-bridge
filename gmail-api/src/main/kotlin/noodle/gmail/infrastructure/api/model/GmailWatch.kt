package noodle.gmail.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
data class GmailWatch(
    val historyId: Long?,
    val expiration: Long?,
    val error: Error?,
) {
    @Serializable
    data class Error(
        val code: Int,
        val message: String,
    )
}
