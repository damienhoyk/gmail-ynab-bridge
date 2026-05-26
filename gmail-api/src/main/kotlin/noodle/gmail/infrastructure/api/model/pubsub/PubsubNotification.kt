package noodle.gmailsync.infrastructure.serialization

import kotlinx.serialization.Serializable

@Serializable
data class PubsubNotification(
    val message: PubsubMessage,
    val subscription: String,
)
