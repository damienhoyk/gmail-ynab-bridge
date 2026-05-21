package noodle.oauth.core.port

import noodle.oauth.core.domain.Token

interface TokenRepository {
    suspend fun getUserId(state: String): String?

    suspend fun getAccessToken(id: String): String?

    suspend fun getRefreshToken(id: String): String?

    suspend fun updateTokenValue(
        id: String,
        type: String,
        value: String,
    ): Token
}
