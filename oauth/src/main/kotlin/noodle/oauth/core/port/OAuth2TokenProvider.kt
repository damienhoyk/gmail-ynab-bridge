package noodle.oauth.core.port

import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.domain.TokenResponse

public interface OAuth2TokenProvider {
    public suspend fun getToken(request: OAuth2TokenRequest): TokenResponse
}
