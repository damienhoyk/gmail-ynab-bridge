package noodle.finance.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import noodle.email.domain.GmailMessageRequest
import noodle.email.infrastructure.api.KtorGmailClient
import noodle.email.infrastructure.serialization.GmailMessage
import noodle.finance.core.port.GmailClient
import noodle.finance.infrastructure.toFinanceDomain

class KtorGmailClientAdapter(httpClient: HttpClient, block: HttpClientConfig<*>.() -> Unit) : KtorGmailClient(httpClient, block), GmailClient {
    override suspend fun getMessage(request: GmailMessageRequest) =
        getMessage(messageId = request.id) {
            parameter("format", request.format.value)
        }.let {
            when (val status = it.status.value) {
                200 -> it.body<GmailMessage>().toFinanceDomain().copy(status = status)
                else -> noodle.finance.core.domain.GmailMessage(status = status)
            }
        }
}
