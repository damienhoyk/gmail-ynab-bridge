package noodle.gmail.infrastructure.api.model.pubsub

import kotlinx.serialization.Serializable

@Serializable
public data class PubsubMessage(
    public val data: String,
    public val messageId: String,
    public val publishTime: String,
)
