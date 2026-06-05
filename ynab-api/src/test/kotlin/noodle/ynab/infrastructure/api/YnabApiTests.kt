package noodle.ynab.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import noodle.ynab.infrastructure.api.model.YnabAccount
import noodle.ynab.infrastructure.api.model.YnabBudget
import noodle.ynab.infrastructure.api.model.YnabTransaction
import noodle.ynab.infrastructure.api.model.YnabUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledInNativeImage
import java.util.UUID

@DisabledInNativeImage
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class YnabApiTests {
    val userId = UUID.randomUUID().toString()
    val budgetId = "4b6ebb6c-2903-4d3f-acb9-a71577202074"
    val budgetName = "My Budget"
    val accountId = "6b1d4897-6b38-4402-a537-3eaf55dcc5bb"
    val accountName = "Checking"
    val transactionId = UUID.randomUUID().toString()
    val transactionAmount = 50000
    val transactionDate = "2024-06-05"
    val transactionPayeeName = "Coffee Shop"

    val engine =
        MockEngine {
            val text =
                when (it.method to it.url.encodedPath) {
                    Get to "/v1/user" ->
                        """
                        {
                          "data": {
                            "user": {
                              "id": "$userId"
                            }
                          }
                        }
                        """.trimIndent()

                    Get to "/v1/budgets" ->
                        """
                        {
                          "data": {
                            "budgets": [
                              {
                                "id": "$budgetId",
                                "name": "$budgetName"
                              }
                            ]
                          }
                        }
                        """.trimIndent()

                    Get to "/v1/budgets/default/accounts" ->
                        """
                        {
                          "data": {
                            "accounts": [
                              {
                                "id": "$accountId",
                                "name": "$accountName"
                              }
                            ]
                          }
                        }
                        """.trimIndent()

                    Post to "/v1/budgets/default/transactions" ->
                        """
                        {
                          "data": {
                            "transaction_ids": ["$transactionId"],
                            "transaction": {
                              "id": "$transactionId",
                              "account_id": "$accountId",
                              "amount": $transactionAmount,
                              "date": "$transactionDate",
                              "payee_name": "$transactionPayeeName"
                            }
                          }
                        }
                        """.trimIndent()

                    else -> "{}"
                }

            when (it.method to it.url.encodedPath) {
                Get to "/v1/user",
                Get to "/v1/budgets",
                Get to "/v1/budgets/default/accounts",
                Post to "/v1/budgets/default/transactions",
                ->
                    respond(
                        content = ByteReadChannel(text),
                        status = OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                else -> respondError(NotFound)
            }
        }

    val httpClient =
        HttpClient(engine) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }

    val ynabApi = YnabApi(httpClient)

    @Test
    fun getUser() =
        runBlocking {
            val user = ynabApi.getUser().body<YnabUser.Data>()
            assertEquals(userId, user.data.user.id)
        }

    @Test
    fun getBudgets() =
        runBlocking {
            val budgets = ynabApi.getBudgets().body<YnabBudget.Data>()
            val budget = budgets.data.budgets.first()
            assertEquals(budgetId, budget.id)
            assertEquals(budgetName, budget.name)
        }

    @Test
    fun getAccounts() =
        runBlocking {
            val accounts = ynabApi.getAccounts().body<YnabAccount.Data>()
            val account = accounts.data.accounts.first()
            assertEquals(accountId, account.id)
            assertEquals(accountName, account.name)
        }

    @Test
    fun postTransactions() =
        runBlocking {
            val result = ynabApi.postTransactions().body<YnabTransaction.Data>()
            assertNotNull(result.data.transaction)
            val transaction = result.data.transaction!!
            assertEquals(transactionId, transaction.id)
            assertEquals(accountId, transaction.accountId)
            assertEquals(transactionAmount, transaction.amount)
            assertEquals(transactionDate, transaction.date)
            assertEquals(transactionPayeeName, transaction.payeeName)
        }
}
