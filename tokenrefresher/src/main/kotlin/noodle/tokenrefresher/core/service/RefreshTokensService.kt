package noodle.tokenrefresher.core.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import noodle.tokenrefresher.core.domain.RefreshableToken
import noodle.tokenrefresher.core.port.OAuth2TokenProvider
import noodle.tokenrefresher.core.port.TokenRepository

public class RefreshTokensService(
    private val tokens: TokenRepository,
    private val providers: Map<String, OAuth2TokenProvider>,
) {
    public suspend fun refreshOne(t: RefreshableToken) {
        val provider = providers[providerOf(t.id)] ?: return
        val resp = provider.refresh(t.refreshToken)
        if (resp.accessToken.isNullOrBlank()) return
        tokens.updateAccess(t.id, resp.accessToken)
        resp.refreshToken?.takeIf { it.isNotBlank() }?.let { tokens.updateRefresh(t.id, it) }
    }

    public suspend fun execute(): Unit =
        tokens.findRefreshable().collect { page ->
            coroutineScope {
                page.map { t -> async { runCatching { refreshOne(t) } } }.awaitAll()
            }
        }

    private fun providerOf(id: String): String =
        when {
            id.endsWith("@gmail.com") -> "google"
            id.endsWith("@app.ynab.com") -> "ynab"
            else -> "unknown"
        }
}
