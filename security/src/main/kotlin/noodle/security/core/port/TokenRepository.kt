package noodle.security.core.port

import noodle.security.core.domain.Token

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
