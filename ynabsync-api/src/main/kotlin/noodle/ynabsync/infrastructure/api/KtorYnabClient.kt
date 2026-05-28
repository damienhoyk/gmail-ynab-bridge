package noodle.ynabsync.infrastructure.api

import io.ktor.client.call.body
import io.ktor.client.request.setBody
import noodle.ynab.infrastructure.api.YnabApi
import noodle.ynab.infrastructure.api.model.YnabAccount
import noodle.ynab.infrastructure.api.model.YnabBudget
import noodle.ynab.infrastructure.api.model.YnabTransaction
import noodle.ynabsync.core.domain.YnabTransactionsRequest
import noodle.ynabsync.core.port.YnabClient
import noodle.ynabsync.infrastructure.api.toFinanceDomain
import noodle.ynabsync.infrastructure.api.toYnabData
import noodle.ynabsync.core.domain.YnabTransaction as YnabTransactionDomain

class KtorYnabClient(
    private val ynabApi: YnabApi,
) : YnabClient {
    suspend fun getUser() = ynabApi.getUser()

    suspend fun getAccounts(budgetId: String = "default") = ynabApi.getAccounts(budgetId) {}.body<YnabAccount.Data>().toFinanceDomain()

    suspend fun getBudgets() = ynabApi.getBudgets {}.body<YnabBudget.Data>().toFinanceDomain()

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
        ynabApi.postTransactions(budgetId) { setBody(body) }
    }
}
