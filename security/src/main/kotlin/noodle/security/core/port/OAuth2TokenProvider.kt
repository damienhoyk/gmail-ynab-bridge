package noodle.security.core.port

import noodle.security.core.domain.OAuth2TokenRequest
import noodle.security.core.domain.TokenResponse

interface OAuth2TokenProvider {
    suspend fun getToken(request: OAuth2TokenRequest): TokenResponse
}
