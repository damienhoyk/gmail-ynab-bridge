package noodle.oauth.core.port

import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.domain.TokenResponse

interface OAuth2TokenProvider {
    suspend fun getToken(request: OAuth2TokenRequest): TokenResponse
}
