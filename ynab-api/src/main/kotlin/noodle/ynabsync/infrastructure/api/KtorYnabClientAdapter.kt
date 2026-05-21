package noodle.ynabsync.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.request.setBody
import noodle.ynabsync.core.domain.YnabTransaction
import noodle.ynabsync.core.domain.YnabTransactionsRequest
import noodle.ynabsync.core.port.YnabClient
import noodle.ynabsync.infrastructure.toYnabData

class KtorYnabClientAdapter(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) : KtorYnabClient(httpClient, block), YnabClient {
    override suspend fun postTransactions(
        budgetId: String,
        transactions: List<YnabTransaction>,
    ) {
        val transactionData =
            transactions.map {
                YnabTransactionsRequest.YnabTransaction(
                    id = it.id,
                    accountId = it.accountId,
                    amount = it.amount,
                    date = it.date,
                    payeeName = it.payeeName,
                ).toYnabData()
            }
        val body =
            noodle.ynabsync.infrastructure.serialization.YnabTransaction.Body(
                transactions = transactionData,
            )
        postTransactions(budgetId) { setBody(body) }
    }
}
