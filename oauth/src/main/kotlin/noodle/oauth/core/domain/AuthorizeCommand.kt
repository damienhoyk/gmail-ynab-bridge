package noodle.oauth.core.domain

public data class AuthorizeCommand(
    val code: String?,
    val state: String?,
)
