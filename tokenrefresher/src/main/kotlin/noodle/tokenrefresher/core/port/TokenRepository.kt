package noodle.tokenrefresher.core.port

import kotlinx.coroutines.flow.Flow
import noodle.tokenrefresher.core.domain.RefreshableToken

public interface TokenRepository {
    public fun findRefreshable(): Flow<List<RefreshableToken>>

    public suspend fun updateAccess(
        id: String,
        value: String,
        expiresIn: Long,
    )

    public suspend fun updateRefresh(
        id: String,
        value: String,
    )
}
