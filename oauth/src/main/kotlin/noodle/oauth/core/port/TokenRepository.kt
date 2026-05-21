package noodle.oauth.core.port

import noodle.oauth.core.domain.Token

interface TokenRepository {
    suspend fun getToken(
        id: String,
        type: String,
    ): Token?

    suspend fun updateTokenValue(
        id: String,
        type: String,
        value: String,
    ): Token
}
