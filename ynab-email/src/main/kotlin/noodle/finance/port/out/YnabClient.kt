package noodle.finance.port.out

import noodle.finance.domain.YnabTransaction

interface YnabClient {
    suspend fun postTransactions(
        budgetId: String = "default",
        transactions: List<YnabTransaction>,
    )
}
