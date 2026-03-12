package noodle.finance.infrastructure.out

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import noodle.finance.domain.YnabTransactionsRequest
import noodle.finance.infrastructure.serialization.YnabAccount
import noodle.finance.infrastructure.serialization.YnabBudget
import noodle.finance.infrastructure.serialization.YnabTransaction
import noodle.finance.infrastructure.toFinanceDomain
import noodle.finance.infrastructure.toYnabData
import noodle.finance.port.out.YnabClient
import noodle.security.domain.TokenResponse
import noodle.security.infrastructure.bearer
import noodle.security.port.out.LoginIdProvider

class KtorYnabClient(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) : YnabClient, LoginIdProvider {
    private val httpClient =
        httpClient.config {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }

            defaultRequest {
                contentType(ContentType.Application.Json)
                url("https://api.ynab.com/v1/")
            }

            install(ContentNegotiation) {
                json(
                    Json { ignoreUnknownKeys = true },
                )
            }

            block()
        }

    suspend fun getUser(block: HttpRequestBuilder.() -> Unit = {}) = httpClient.get("user", block)

    suspend fun getAccounts(
        budgetId: String = "default",
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.get("budgets/$budgetId/accounts", block).body<YnabAccount.Data>()

    suspend fun getAccounts(budgetId: String) = getAccounts(budgetId) {}.toFinanceDomain()

    suspend fun getBudgets(block: HttpRequestBuilder.() -> Unit = {}) =
        httpClient.get("budgets", block).body<YnabBudget.Data>()

    suspend fun getBudgets() = getBudgets {}.toFinanceDomain()

    suspend fun postTransactions(
        budgetId: String = "default",
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.post("budgets/$budgetId/transactions", block)

    override suspend fun postTransactions(
        budgetId: String,
        transactions: List<YnabTransactionsRequest.YnabTransaction>,
    ) {
        val transactions = transactions.map { it.toYnabData() }
        val body = YnabTransaction.Body(transactions = transactions)
        postTransactions(budgetId) { setBody(body) }
    }

    override suspend fun getLoginId(response: TokenResponse): String? {
        val client =
            response.accessToken?.let {
                KtorYnabClient(httpClient) { install(Auth) { bearer(it) } }
            }

        val response = client?.getUser()?.body<JsonObject>()
        val id =
            response?.get("data")
                ?.jsonObject
                ?.get("user")
                ?.jsonObject
                ?.get("id")
                ?.jsonPrimitive
                ?.content
        return id?.let { "$it@app.ynab.com" }
    }
}
