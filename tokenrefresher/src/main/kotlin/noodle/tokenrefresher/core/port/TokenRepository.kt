package noodle.tokenrefresher.core.port

import noodle.tokenrefresher.core.domain.RefreshableToken

public interface TokenRepository {
    public suspend fun findRefreshable(): List<RefreshableToken>

    public suspend fun updateAccess(
        id: String,
        value: String,
    )

    public suspend fun updateRefresh(
        id: String,
        value: String,
    )
}
