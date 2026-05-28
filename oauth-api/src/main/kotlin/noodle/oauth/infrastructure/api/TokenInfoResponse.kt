package noodle.oauth.infrastructure.api

import kotlinx.serialization.Serializable

@Serializable
data class TokenInfoResponse(
    val email: String? = null,
)
