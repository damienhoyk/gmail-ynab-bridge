package noodle.telegramchat.core.domain

public data class Mailbox(
    val address: String,
    val state: Long?,
    val expiration: Long?,
)
