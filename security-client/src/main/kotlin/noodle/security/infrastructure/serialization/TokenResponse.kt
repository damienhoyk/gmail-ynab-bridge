package noodle.security.infrastructure.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import noodle.security.domain.TokenResponse

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null,
    val error: String? = null,
) {
    fun domain() =
        TokenResponse(
            accessToken = accessToken,
            idToken = idToken,
            refreshToken = refreshToken,
            expiresIn = expiresIn,
            error = error,
        )
}
