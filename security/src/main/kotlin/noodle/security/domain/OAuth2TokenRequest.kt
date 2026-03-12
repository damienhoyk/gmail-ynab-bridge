package noodle.security.domain

data class OAuth2TokenRequest(
    val code: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val redirectUri: String? = null,
    val refreshToken: String? = null,
    val grantType: String? = "authorization_code",
)
