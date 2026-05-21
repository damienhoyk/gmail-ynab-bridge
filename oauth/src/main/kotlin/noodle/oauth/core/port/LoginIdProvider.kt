package noodle.oauth.core.port

import noodle.oauth.core.domain.TokenResponse

interface LoginIdProvider {
    suspend fun getLoginId(response: TokenResponse): String?
}
