package noodle.telegramchat.core.domain

import kotlin.time.Duration

public data class StateToken(
    val id: String,
    val userId: String?,
    val duration: Duration,
)
