package noodle.google.event

import kotlinx.serialization.Serializable

@Serializable
data class GmailEvent(val emailAddress: String, val historyId: Long)