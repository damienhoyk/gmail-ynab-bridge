package noodle.chat.domain

import kotlin.time.Duration

data class StateToken(val id: String, val userId: String?, val duration: Duration)
