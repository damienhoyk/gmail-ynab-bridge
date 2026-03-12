package noodle.finance.port.out

import noodle.finance.domain.YnabTransactionsRequest

interface YnabClient {
    suspend fun postTransactions(
        budgetId: String = "default",
        transactions: List<YnabTransactionsRequest.YnabTransaction>,
    )
}
