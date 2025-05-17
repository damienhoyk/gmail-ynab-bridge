package noodle.google.gmail

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val emailAddress: String,
    val historyId: Long
)