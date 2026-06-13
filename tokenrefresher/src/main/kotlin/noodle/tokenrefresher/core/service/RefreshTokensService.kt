package noodle.tokenrefresher.core.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import noodle.tokenrefresher.core.domain.RefreshableToken
import noodle.tokenrefresher.core.port.OAuth2TokenProvider
import noodle.tokenrefresher.core.port.TokenRepository

public class RefreshTokensService(
    private val tokens: TokenRepository,
    private val providers: Map<String, OAuth2TokenProvider>,
    private val maxConcurrency: Int = 5,
) {
    public suspend fun discover(): List<RefreshableToken> = tokens.findRefreshable()

    public suspend fun refreshOne(t: RefreshableToken) {
        val provider = providers[providerOf(t.id)] ?: return
        val resp = provider.refresh(t.refreshToken)
        if (resp.accessToken.isNullOrBlank()) return
        tokens.updateAccess(t.id, resp.accessToken)
        resp.refreshToken?.takeIf { it.isNotBlank() }?.let { tokens.updateRefresh(t.id, it) }
    }

    public suspend fun execute(): Unit =
        coroutineScope {
            val gate = Semaphore(maxConcurrency)
            discover().map { t -> async { gate.withPermit { runCatching { refreshOne(t) } } } }.awaitAll()
        }

    private fun providerOf(id: String): String =
        when {
            id.endsWith("@gmail.com") -> "google"
            id.endsWith("@app.ynab.com") -> "ynab"
            else -> "unknown"
        }
}
