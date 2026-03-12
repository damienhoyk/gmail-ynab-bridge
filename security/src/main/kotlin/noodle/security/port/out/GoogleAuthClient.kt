package noodle.security.port.out

import noodle.security.domain.OAuth2TokenRequest
import noodle.security.domain.TokenInfoResponse
import noodle.security.domain.TokenResponse

interface GoogleAuthClient {
    suspend fun getToken(request: OAuth2TokenRequest): TokenResponse

    suspend fun getTokenInfo(token: String): TokenInfoResponse
}
