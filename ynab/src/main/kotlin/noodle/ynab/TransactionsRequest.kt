package noodle.ynab

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody

data class TransactionsRequest(private val body: Transaction.Body) {

    val block: HttpRequestBuilder.() -> Unit = {
        setBody(this@TransactionsRequest.body)
    }

}