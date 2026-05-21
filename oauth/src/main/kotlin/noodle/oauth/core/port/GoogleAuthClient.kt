package noodle.oauth.core.port

import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.domain.TokenInfoResponse
import noodle.oauth.core.domain.TokenResponse

interface GoogleAuthClient {
    suspend fun getToken(request: OAuth2TokenRequest): TokenResponse

    suspend fun getTokenInfo(token: String): TokenInfoResponse
}
