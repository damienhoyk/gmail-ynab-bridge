package noodle.ynabsync.core.port

import noodle.ynabsync.core.domain.YnabTransaction

interface YnabClient {
    suspend fun postTransactions(
        budgetId: String = "default",
        transactions: List<YnabTransaction>,
    )
}
