package noodle.oauth.infrastructure.api.ynab

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import noodle.oauth.core.domain.TokenResponse
import noodle.oauth.core.port.LoginIdProvider
import noodle.ynab.infrastructure.api.YnabApi
import noodle.ynab.infrastructure.api.model.YnabUser

public class KtorYnabLoginIdProvider(
    private val ynabApi: YnabApi,
) : LoginIdProvider {
    public override suspend fun getLoginId(response: TokenResponse): String? =
        response.accessToken?.let { accessToken ->
            ynabApi
                .getUser { bearerAuth(accessToken) }
                .body<YnabUser.Data>()
                .data.user.id
                .let { "$it@app.ynab.com" }
        }
}
