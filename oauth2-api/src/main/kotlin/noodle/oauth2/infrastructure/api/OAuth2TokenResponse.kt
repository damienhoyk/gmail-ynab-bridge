package noodle.oauth2.infrastructure.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class OAuth2TokenResponse(
    @SerialName("access_token") public val accessToken: String? = null,
    @SerialName("id_token") public val idToken: String? = null,
    @SerialName("refresh_token") public val refreshToken: String? = null,
    @SerialName("expires_in") public val expiresIn: Int? = null,
    public val error: String? = null,
)
