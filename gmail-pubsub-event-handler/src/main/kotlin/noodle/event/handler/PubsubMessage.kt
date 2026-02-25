package noodle.event.handler

import kotlinx.serialization.Serializable

@Serializable
data class PubsubMessage(val data: String, val messageId: String, val publishTime: String)