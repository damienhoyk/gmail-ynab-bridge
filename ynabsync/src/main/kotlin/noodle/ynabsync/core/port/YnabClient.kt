package noodle.ynabsync.core.port

import noodle.ynabsync.core.domain.YnabTransaction

public interface YnabClient {
    public suspend fun postTransactions(
        budgetId: String = "default",
        transactions: List<YnabTransaction>,
    )
}
