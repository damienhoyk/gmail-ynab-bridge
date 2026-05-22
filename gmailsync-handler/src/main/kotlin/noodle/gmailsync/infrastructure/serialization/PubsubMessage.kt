package noodle.gmailsync.infrastructure.serialization

import kotlinx.serialization.Serializable

@Serializable
data class PubsubMessage(
    val data: String,
    val messageId: String,
    val publishTime: String,
)
