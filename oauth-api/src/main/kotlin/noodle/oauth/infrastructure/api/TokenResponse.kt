package noodle.oauth.infrastructure.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import noodle.oauth.core.domain.TokenResponse

@Serializable
public data class TokenResponse(
    @SerialName("access_token") public val accessToken: String? = null,
    @SerialName("id_token") public val idToken: String? = null,
    @SerialName("refresh_token") public val refreshToken: String? = null,
    @SerialName("expires_in") public val expiresIn: Int? = null,
    public val error: String? = null,
) {
    public fun domain(): noodle.oauth.core.domain.TokenResponse =
        TokenResponse(
            accessToken = accessToken,
            idToken = idToken,
            refreshToken = refreshToken,
            expiresIn = expiresIn,
            error = error,
        )
}
