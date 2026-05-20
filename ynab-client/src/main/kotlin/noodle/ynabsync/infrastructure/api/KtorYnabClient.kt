package noodle.ynabsync.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import noodle.ynabsync.infrastructure.serialization.YnabAccount
import noodle.ynabsync.infrastructure.serialization.YnabBudget
import noodle.ynabsync.infrastructure.toFinanceDomain

open class KtorYnabClient(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    protected val httpClient =
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
}
