package noodle.gmail.infrastructure.api.model.pubsub

import kotlinx.serialization.Serializable

@Serializable
data class PubsubNotification(
    val message: PubsubMessage,
    val subscription: String,
)
