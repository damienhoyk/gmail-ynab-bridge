package noodle.ynabsync.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledInNativeImage
import java.util.UUID.randomUUID

@DisabledInNativeImage
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorYnabClientTests {
    val id = randomUUID().toString()
    val budgetId = randomUUID().toString()
    val accountId = randomUUID().toString()
    val accountName = "🏦 ${randomUUID()}"
    val budgetName = "📔 ${randomUUID()}"

    val engine =
        MockEngine {
            val text =
                when (it.method to it.url.encodedPath) {
                    Get to "/v1/user" ->
                        """
                        {
                          "data": {
                            "user": {
                              "id": "$id"
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
                                "name": "$budgetName",
                                "last_modified_on": "2026-02-28T15:49:46.531Z",
                                "first_month": "2026-02-28",
                                "last_month": "2026-02-28",
                                "date_format": {
                                  "format": "string"
                                },
                                "currency_format": {
                                  "iso_code": "string",
                                  "example_format": "string",
                                  "decimal_digits": 1,
                                  "decimal_separator": "string",
                                  "symbol_first": true,
                                  "group_separator": "string",
                                  "currency_symbol": "string",
                                  "display_symbol": true
                                },
                                "accounts": [
                                  {
                                    "id": "$accountId",
                                    "name": "$accountName",
                                    "type": "checking",
                                    "on_budget": true,
                                    "closed": true,
                                    "note": null,
                                    "balance": 1,
                                    "cleared_balance": 1,
                                    "uncleared_balance": 1,
                                    "transfer_payee_id": null,
                                    "...": "[Additional Properties Truncated]"
                                  }
                                ]
                              }
                            ],
                            "default_budget": {
                              "id": "$budgetId",
                              "name": "$budgetName",
                              "last_modified_on": "2026-02-28T15:49:46.531Z",
                              "first_month": "2026-02-28",
                              "last_month": "2026-02-28",
                              "date_format": {
                                "format": "string"
                              },
                              "currency_format": {
                                "iso_code": "string",
                                "example_format": "string",
                                "decimal_digits": 1,
                                "decimal_separator": "string",
                                "symbol_first": true,
                                "group_separator": "string",
                                "currency_symbol": "string",
                                "display_symbol": true
                              },
                              "accounts": [
                                {
                                  "id": "$accountId",
                                  "name": "$accountName",
                                  "type": "checking",
                                  "on_budget": true,
                                  "closed": true,
                                  "note": null,
                                  "balance": 1,
                                  "cleared_balance": 1,
                                  "uncleared_balance": 1,
                                  "transfer_payee_id": null
                                }
                              ]
                            }
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
                                "name": "$accountName",
                                "type": "checking",
                                "on_budget": true,
                                "closed": true,
                                "note": null,
                                "balance": 1,
                                "cleared_balance": 1,
                                "uncleared_balance": 1,
                                "transfer_payee_id": null,
                                "direct_import_linked": true,
                                "direct_import_in_error": true,
                                "last_reconciled_at": null,
                                "debt_original_balance": null,
                                "debt_interest_rates": null,
                                "debt_minimum_payments": null,
                                "debt_escrow_amounts": null,
                                "deleted": true
                              }
                            ],
                            "server_knowledge": 1
                          }
                        }
                        """.trimIndent()

                    else -> "{}"
                }

            when (it.method to it.url.encodedPath) {
                Get to "/v1/user",
                Get to "/v1/budgets",
                Get to "/v1/budgets/default/accounts",
                ->
                    respond(
                        content = ByteReadChannel(text),
                        status = OK,
                        headers = headersOf(ContentType, "application/json"),
                    )

                else -> respondError(HttpStatusCode.NotFound)
            }
        }

    val httpClient =
        HttpClient(engine) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }
    val ynabClient = KtorYnabClientAdapter(httpClient)

    @Test
    fun getUser(): Unit =
        runBlocking {
            val userJson = ynabClient.getUser().body<JsonObject>()
            val user = userJson["data"]?.jsonObject["user"]?.jsonObject
            Assertions.assertEquals(id, user?.get("id")?.jsonPrimitive?.content)
        }

    @Test
    fun getBudgets(): Unit =
        runBlocking {
            val budgets = ynabClient.getBudgets()
            val budget = budgets.data.budgets.first()
            Assertions.assertEquals(budgetId, budget.id)
            Assertions.assertEquals(budgetName, budget.name)
        }

    @Test
    fun getAccounts() {
        val accounts = runBlocking { ynabClient.getAccounts() }
        val account = accounts.data.accounts.first()
        Assertions.assertEquals(accountId, account.id)
        Assertions.assertEquals(accountName, account.name)
    }
}
