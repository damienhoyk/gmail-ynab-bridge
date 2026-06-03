package noodle.telegramchat.core.domain

public data class GmailWatch(
    val historyId: Long?,
    val expiration: Long?,
    val error: Error?,
) {
    public data class Error(
        val code: Int,
        val message: String,
    )
}
