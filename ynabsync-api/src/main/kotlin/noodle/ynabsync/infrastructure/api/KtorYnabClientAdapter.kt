package noodle.ynabsync.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import noodle.ynab.infrastructure.api.KtorYnabClient
import noodle.ynab.infrastructure.api.model.YnabAccount
import noodle.ynab.infrastructure.api.model.YnabBudget
import noodle.ynab.infrastructure.api.model.YnabTransaction
import noodle.ynabsync.core.domain.YnabTransactionsRequest
import noodle.ynabsync.core.port.YnabClient
import noodle.ynabsync.infrastructure.toFinanceDomain
import noodle.ynabsync.infrastructure.toYnabData
import noodle.ynabsync.core.domain.YnabTransaction as YnabTransactionDomain

class KtorYnabClientAdapter(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) : KtorYnabClient(httpClient, block),
    YnabClient {
    suspend fun getAccounts(budgetId: String = "default") = getAccounts(budgetId) {}.body<YnabAccount.Data>().toFinanceDomain()

    suspend fun getBudgets() = getBudgets {}.body<YnabBudget.Data>().toFinanceDomain()

    override suspend fun postTransactions(
        budgetId: String,
        transactions: List<YnabTransactionDomain>,
    ) {
        val transactionData =
            transactions.map {
                YnabTransactionsRequest
                    .YnabTransaction(
                        id = it.id,
                        accountId = it.accountId,
                        amount = it.amount,
                        date = it.date,
                        payeeName = it.payeeName,
                    ).toYnabData()
            }
        val body =
            YnabTransaction.Body(
                transactions = transactionData,
            )
        postTransactions(budgetId) { setBody(body) }
    }
}
