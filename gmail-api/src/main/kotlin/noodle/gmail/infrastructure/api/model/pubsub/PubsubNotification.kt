package noodle.gmail.infrastructure.api.model.pubsub

import kotlinx.serialization.Serializable

@Serializable
public data class PubsubNotification(
    public val message: PubsubMessage,
    public val subscription: String,
)
