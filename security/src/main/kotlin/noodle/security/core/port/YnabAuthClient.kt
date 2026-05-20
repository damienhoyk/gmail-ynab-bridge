package noodle.security.core.port

import noodle.security.core.domain.OAuth2TokenRequest
import noodle.security.core.domain.TokenResponse

interface YnabAuthClient {
    suspend fun getToken(request: OAuth2TokenRequest): TokenResponse
}
