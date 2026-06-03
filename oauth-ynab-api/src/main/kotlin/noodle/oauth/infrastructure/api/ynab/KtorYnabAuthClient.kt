package noodle.oauth.infrastructure.api.ynab

import kotlinx.coroutines.Deferred
import noodle.oauth.infrastructure.api.KtorOAuth2TokenProvider
import noodle.ynab.auth.infrastructure.api.YnabAuthApi

public class KtorYnabAuthClient(
    private val ynabAuthApi: YnabAuthApi,
) : KtorOAuth2TokenProvider() {
    public override val httpClient: io.ktor.client.HttpClient
        get() = ynabAuthApi.httpClient

    public override val tokenEndpoint: Deferred<String>
        get() = ynabAuthApi.tokenEndpoint
}
