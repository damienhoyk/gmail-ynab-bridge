package noodle.tokenrefresher.core.port

import noodle.tokenrefresher.core.domain.TokenResponse

public interface OAuth2TokenProvider {
    public suspend fun refresh(refreshToken: String): TokenResponse
}
