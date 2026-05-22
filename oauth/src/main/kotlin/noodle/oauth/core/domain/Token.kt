package noodle.oauth.core.domain

data class Token(
    val id: String,
    val type: String,
    val value: String,
)
