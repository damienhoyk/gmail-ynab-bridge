package noodle.oauth.core.port

import noodle.oauth.core.domain.Token

public interface TokenRepository {
    public suspend fun getUserId(state: String): String?

    public suspend fun getAccessToken(id: String): String?

    public suspend fun getRefreshToken(id: String): String?

    public suspend fun updateTokenValue(
        id: String,
        type: String,
        value: String,
    ): Token
}
