package noodle.oauth.infrastructure.api.ynab

import kotlinx.coroutines.Deferred
import noodle.oauth.core.port.YnabAuthClient
import noodle.oauth.infrastructure.api.KtorOAuth2TokenProvider
import noodle.ynab.auth.infrastructure.api.YnabAuthApi

class KtorYnabAuthClient(
    private val ynabAuthApi: YnabAuthApi,
) : KtorOAuth2TokenProvider(),
    YnabAuthClient {
    override val httpClient
        get() = ynabAuthApi.httpClient

    override val tokenEndpoint: Deferred<String>
        get() = ynabAuthApi.tokenEndpoint
}
