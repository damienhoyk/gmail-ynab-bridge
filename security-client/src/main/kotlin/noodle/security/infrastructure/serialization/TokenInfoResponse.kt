package noodle.security.infrastructure.serialization

import kotlinx.serialization.Serializable
import noodle.security.domain.TokenInfoResponse

@Serializable
data class TokenInfoResponse(val email: String? = null) {
    fun domain() = TokenInfoResponse(email = email)
}
