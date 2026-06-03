package noodle.ynabsync.core.domain

public data class YnabTransactionsRequest(
    public val body: Body,
) {
    public data class YnabTransaction(
        public val id: String? = null,
        public val accountId: String? = null,
        public val amount: Int? = null,
        public val date: String? = null,
        public val payeeName: String? = null,
    )

    public data class Body(
        public val transactionIds: List<String>? = emptyList(),
        public val transaction: YnabTransaction? = null,
        public val transactions: List<YnabTransaction>? = emptyList(),
    )

    public data class Data(
        public val data: Body,
    )
}
