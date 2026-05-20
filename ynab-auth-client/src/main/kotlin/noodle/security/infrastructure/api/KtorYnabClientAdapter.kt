package noodle.security.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import noodle.finance.infrastructure.api.KtorYnabClient
import noodle.security.core.domain.TokenResponse
import noodle.security.core.port.LoginIdProvider
import noodle.security.infrastructure.bearer

class KtorYnabClientAdapter(
    private val httpClient: HttpClient,
    private val block: HttpClientConfig<*>.() -> Unit = {},
) : LoginIdProvider {
    override suspend fun getLoginId(response: TokenResponse): String? {
        val client =
            response.accessToken?.let {
                KtorYnabClient(httpClient) { install(Auth) { bearer(it) } }
            }
        val body = client?.getUser()?.body<JsonObject>()
        val id =
            body
                ?.get("data")?.jsonObject
                ?.get("user")?.jsonObject
                ?.get("id")?.jsonPrimitive?.content
        return id?.let { "$it@app.ynab.com" }
    }
}
