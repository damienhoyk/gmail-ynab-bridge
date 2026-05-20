package noodle.finance.infrastructure.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YnabTransaction(
    val id: String? = null,
    @SerialName("account_id") val accountId: String? = null,
    val amount: Int? = null,
    val date: String? = null,
    @SerialName("payee_name") val payeeName: String? = null,
) {
    @Serializable
    data class Body(
        @SerialName("transaction_ids") val transactionIds: List<String>? = emptyList(),
        val transaction: YnabTransaction? = null,
        val transactions: List<YnabTransaction>? = emptyList(),
    )

    @Serializable
    data class Data(val data: Body)
}
