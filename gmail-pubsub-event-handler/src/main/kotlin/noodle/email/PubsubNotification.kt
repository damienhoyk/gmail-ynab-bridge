package noodle.email

import kotlinx.serialization.Serializable

@Serializable
data class PubsubNotification(val message: PubsubMessage, val subscription: String)
