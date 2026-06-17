package noodle.google.auth.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
public data class TokenInfoResponse(
    val sub: String? = null,
    val email: String? = null,
)
