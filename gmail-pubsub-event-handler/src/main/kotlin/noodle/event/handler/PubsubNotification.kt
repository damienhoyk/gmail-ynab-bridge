package noodle.event.handler

import kotlinx.serialization.Serializable

@Serializable
data class PubsubNotification(val message: PubsubMessage, val subscription: String)
