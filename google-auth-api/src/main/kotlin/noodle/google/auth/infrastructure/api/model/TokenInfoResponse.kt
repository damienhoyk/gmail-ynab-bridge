package noodle.google.auth.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
public data class TokenInfoResponse(
    val email: String? = null,
)
