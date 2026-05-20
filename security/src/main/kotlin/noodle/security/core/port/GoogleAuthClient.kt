package noodle.security.core.port

import noodle.security.core.domain.OAuth2TokenRequest
import noodle.security.core.domain.TokenInfoResponse
import noodle.security.core.domain.TokenResponse

interface GoogleAuthClient {
    suspend fun getToken(request: OAuth2TokenRequest): TokenResponse

    suspend fun getTokenInfo(token: String): TokenInfoResponse
}
