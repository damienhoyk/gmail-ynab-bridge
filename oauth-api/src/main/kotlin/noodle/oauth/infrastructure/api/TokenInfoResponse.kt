package noodle.oauth.infrastructure.api

import kotlinx.serialization.Serializable
import noodle.oauth.core.domain.TokenInfoResponse

@Serializable
data class TokenInfoResponse(
    val email: String? = null,
) {
    fun domain() = TokenInfoResponse(email = email)
}
