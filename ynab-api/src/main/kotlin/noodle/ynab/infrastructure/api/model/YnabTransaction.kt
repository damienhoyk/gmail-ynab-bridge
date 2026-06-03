package noodle.ynab.infrastructure.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class YnabTransaction(
    public val id: String? = null,
    @SerialName("account_id") val accountId: String? = null,
    public val amount: Int? = null,
    public val date: String? = null,
    @SerialName("payee_name") val payeeName: String? = null,
) {
    @Serializable
    public data class Body(
        @SerialName("transaction_ids") val transactionIds: List<String>? = emptyList(),
        public val transaction: YnabTransaction? = null,
        public val transactions: List<YnabTransaction>? = emptyList(),
    )

    @Serializable
    public data class Data(
        public val data: Body,
    )
}
