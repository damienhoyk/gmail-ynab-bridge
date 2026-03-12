package noodle.security.domain

data class TokenResponse(
    val accessToken: String? = null,
    val idToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val error: String? = null,
)
