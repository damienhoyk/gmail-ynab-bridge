package noodle.security.port.out

import noodle.security.domain.TokenResponse

interface LoginIdProvider {
    suspend fun getLoginId(response: TokenResponse): String?
}
