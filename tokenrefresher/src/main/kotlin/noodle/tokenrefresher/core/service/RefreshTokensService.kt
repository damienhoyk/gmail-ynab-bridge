package noodle.tokenrefresher.core.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import noodle.tokenrefresher.core.domain.RefreshableToken
import noodle.tokenrefresher.core.port.OAuth2TokenProvider
import noodle.tokenrefresher.core.port.TokenRepository

public class RefreshTokensService(
    private val tokens: TokenRepository,
    private val provider: suspend (String) -> OAuth2TokenProvider?,
) {
    public suspend fun refreshOne(token: RefreshableToken) {
        val provider = provider(token.id.substringAfter('@')) ?: return
        val response = provider.refresh(token.refreshToken)
        if (response.accessToken.isNullOrBlank()) return
        tokens.updateAccess(token.id, response.accessToken)
        response.refreshToken?.takeIf { it.isNotBlank() }?.let { tokens.updateRefresh(token.id, it) }
    }

    public suspend fun execute(): Unit =
        tokens.findRefreshable().collect { page ->
            coroutineScope {
                page.map { async { runCatching { refreshOne(it) } } }.awaitAll()
            }
        }
}
