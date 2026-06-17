package noodle.oauth.core.domain

public data class LoginIdentity(
    public val id: String,
    public val aliases: List<String> = emptyList(),
)
