package noodle.security.core.port

import noodle.security.core.domain.TokenResponse

interface LoginIdProvider {
    suspend fun getLoginId(response: TokenResponse): String?
}
