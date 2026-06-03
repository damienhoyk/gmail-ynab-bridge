package noodle.ynabsync.infrastructure.api

import io.ktor.client.call.body
import io.ktor.client.request.setBody
import noodle.ynab.infrastructure.api.YnabApi
import noodle.ynab.infrastructure.api.model.YnabAccount
import noodle.ynab.infrastructure.api.model.YnabBudget
import noodle.ynab.infrastructure.api.model.YnabTransaction
import noodle.ynabsync.core.domain.YnabTransactionsRequest
import noodle.ynabsync.core.port.YnabClient
import noodle.ynabsync.core.domain.YnabTransaction as YnabTransactionDomain

public class KtorYnabClient(
    private val ynabApi: YnabApi,
) : YnabClient {
    public suspend fun getUser(): io.ktor.client.statement.HttpResponse = ynabApi.getUser()

    public suspend fun getAccounts(budgetId: String = "default"): noodle.ynabsync.core.domain.YnabAccount.Data = ynabApi.getAccounts(budgetId) {}.body<YnabAccount.Data>().toFinanceDomain()

    public suspend fun getBudgets(): noodle.ynabsync.core.domain.YnabBudget.Data = ynabApi.getBudgets {}.body<YnabBudget.Data>().toFinanceDomain()

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

private fun YnabAccount.toFinanceDomain() =
    noodle.ynabsync.core.domain
        .YnabAccount(id = id, name = name)

private fun YnabAccount.Body.toFinanceDomain() =
    noodle.ynabsync.core.domain.YnabAccount
        .Body(accounts = accounts.map { it.toFinanceDomain() })

private fun YnabAccount.Data.toFinanceDomain() =
    noodle.ynabsync.core.domain.YnabAccount
        .Data(data = data.toFinanceDomain())

private fun YnabBudget.toFinanceDomain() =
    noodle.ynabsync.core.domain
        .YnabBudget(id = id, name = name)

private fun YnabBudget.Body.toFinanceDomain() =
    noodle.ynabsync.core.domain.YnabBudget
        .Body(budgets = budgets.map { it.toFinanceDomain() })

private fun YnabBudget.Data.toFinanceDomain() =
    noodle.ynabsync.core.domain.YnabBudget
        .Data(data = data.toFinanceDomain())

private fun YnabTransactionsRequest.YnabTransaction.toYnabData() =
    YnabTransaction(
        id = id,
        accountId = accountId,
        amount = amount,
        date = date,
        payeeName = payeeName,
    )
