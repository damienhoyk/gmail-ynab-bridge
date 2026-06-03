package noodle.oauth.core.port

import noodle.oauth.core.domain.TokenResponse

public interface LoginIdProvider {
    public suspend fun getLoginId(response: TokenResponse): String?
}
