package noodle.finance

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody

data class YnabTransactionsRequest(private val body: YnabTransaction.Body) {

    val block: HttpRequestBuilder.() -> Unit = {
        setBody(this@YnabTransactionsRequest.body)
    }

}