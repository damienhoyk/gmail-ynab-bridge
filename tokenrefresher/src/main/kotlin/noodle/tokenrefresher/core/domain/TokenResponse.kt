package noodle.tokenrefresher.core.domain

public data class TokenResponse(
    val accessToken: String?,
    val refreshToken: String?,
    val expiresIn: Long,
)
