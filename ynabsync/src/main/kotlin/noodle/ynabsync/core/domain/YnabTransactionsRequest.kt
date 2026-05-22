package noodle.ynabsync.core.domain

data class YnabTransactionsRequest(
    val body: Body,
) {
    data class YnabTransaction(
        val id: String? = null,
        val accountId: String? = null,
        val amount: Int? = null,
        val date: String? = null,
        val payeeName: String? = null,
    )

    data class Body(
        val transactionIds: List<String>? = emptyList(),
        val transaction: YnabTransaction? = null,
        val transactions: List<YnabTransaction>? = emptyList(),
    )

    data class Data(
        val data: Body,
    )
}
