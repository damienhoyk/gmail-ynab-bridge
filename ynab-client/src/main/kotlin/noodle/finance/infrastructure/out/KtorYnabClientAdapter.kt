package noodle.finance.infrastructure.out

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.request.setBody
import noodle.finance.domain.YnabTransaction
import noodle.finance.domain.YnabTransactionsRequest
import noodle.finance.infrastructure.toYnabData
import noodle.finance.port.out.YnabClient

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
            noodle.finance.infrastructure.serialization.YnabTransaction.Body(
                transactions = transactionData,
            )
        postTransactions(budgetId) { setBody(body) }
    }
}
