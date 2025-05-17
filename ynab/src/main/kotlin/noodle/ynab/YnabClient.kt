package noodle.ynab

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.ContentType.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import noodle.home.security.AccessTokenProvider

class YnabClient(block: BearerAuthConfig.() -> Unit = {}) {

    constructor(accessToken: String): this({
        loadTokens {
            BearerTokens(accessToken, null)
        }
    })

    constructor(id: String, accessTokenProvider: AccessTokenProvider) : this({

        loadTokens {
            val accessToken = accessTokenProvider.getToken(id)
            BearerTokens(accessToken, null)
        }

        refreshTokens {
            val accessToken = accessTokenProvider.getNewToken(id)
            BearerTokens(accessToken, null)
        }

    })

    val httpClient = HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        defaultRequest {
            contentType(Application.Json)
            url("https://api.ynab.com/v1/")
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        install(Auth) {
            bearer(block)
        }
    }

    suspend fun getUser(block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("user", block)

    suspend fun getAccounts(budgetId: String = "default", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("budgets/$budgetId/accounts", block)

    suspend fun getBudgets(block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("budgets", block)

    suspend fun postTransactions(budgetId: String = "default", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .post("budgets/$budgetId/transactions", block)

    suspend fun postTransactions(budgetId: String = "default", request: TransactionsRequest) =
        postTransactions(budgetId, request.block)

}
