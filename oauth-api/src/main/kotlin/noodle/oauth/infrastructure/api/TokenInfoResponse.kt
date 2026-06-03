package noodle.oauth.infrastructure.api

import kotlinx.serialization.Serializable

@Serializable
public data class TokenInfoResponse(
    public val email: String? = null,
)
