package noodle.ynab.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType.Application
import io.ktor.http.contentType
import noodle.ktor.defaultJson
import noodle.ktor.defaultLogging

public class YnabApi(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val urlString = "https://api.ynab.com/v1/"
    private val httpClient =
        httpClient.config {
            defaultLogging()
            defaultJson()
            defaultRequest {
                contentType(Application.Json)
                url(urlString)
            }

            block()
        }

    public suspend fun getUser(block: HttpRequestBuilder.() -> Unit = {}): HttpResponse = httpClient.get("user", block)

    public suspend fun getAccounts(
        budgetId: String = "default",
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = httpClient.get("budgets/$budgetId/accounts", block)

    public suspend fun getBudgets(block: HttpRequestBuilder.() -> Unit = {}): HttpResponse = httpClient.get("budgets", block)

    public suspend fun postTransactions(
        budgetId: String = "default",
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = httpClient.post("budgets/$budgetId/transactions", block)
}
