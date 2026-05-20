package noodle.finance.core.port

import noodle.finance.core.domain.YnabTransaction

interface YnabClient {
    suspend fun postTransactions(
        budgetId: String = "default",
        transactions: List<YnabTransaction>,
    )
}
