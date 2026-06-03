package noodle.oauth.core.domain

public data class Token(
    val id: String,
    val type: String,
    val value: String,
)
