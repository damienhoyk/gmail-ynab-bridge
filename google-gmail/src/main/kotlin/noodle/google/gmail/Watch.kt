package noodle.google.gmail

import kotlinx.serialization.Serializable

@Serializable
data class Watch(
    val historyId: Long?,
    val expiration: Long?,
    val error: Error?
) {

    @Serializable
    data class Error(val code: Int, val message: String)

}

