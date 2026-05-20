package noodle.chat.core.domain

data class GmailWatch(
    val historyId: Long?,
    val expiration: Long?,
    val error: Error?,
) {
    data class Error(val code: Int, val message: String)
}
