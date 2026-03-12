package noodle.security.port.out

import noodle.security.domain.OAuth2TokenRequest
import noodle.security.domain.TokenResponse

interface YnabAuthClient {
    suspend fun getToken(request: OAuth2TokenRequest): TokenResponse
}
