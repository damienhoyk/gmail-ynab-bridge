package noodle.oauth.infrastructure.api.ynab

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import noodle.oauth.core.domain.TokenResponse
import noodle.oauth.core.port.LoginIdProvider
import noodle.oauth.infrastructure.api.bearer
import noodle.ynab.infrastructure.api.YnabApi
import noodle.ynab.infrastructure.api.model.YnabUser

class KtorYnabLoginIdProvider(
    private val httpClient: HttpClient,
) : LoginIdProvider {
    override suspend fun getLoginId(response: TokenResponse): String? =
        response.accessToken?.let { accessToken ->
            YnabApi(httpClient) { install(Auth) { bearer(accessToken) } }
                .getUser()
                .body<YnabUser.Data>()
                .data.user.id
                .let { "$it@app.ynab.com" }
        }
}
