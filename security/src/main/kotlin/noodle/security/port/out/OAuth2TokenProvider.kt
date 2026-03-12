package noodle.security.port.out

import noodle.security.domain.OAuth2TokenRequest
import noodle.security.domain.TokenResponse

interface OAuth2TokenProvider {
    suspend fun getToken(request: OAuth2TokenRequest): TokenResponse
}
