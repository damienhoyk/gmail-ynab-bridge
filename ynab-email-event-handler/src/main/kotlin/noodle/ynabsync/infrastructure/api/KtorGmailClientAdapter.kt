package noodle.ynabsync.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import noodle.gmailsync.core.domain.GmailMessageRequest
import noodle.gmailsync.infrastructure.api.KtorGmailClient
import noodle.gmailsync.infrastructure.serialization.GmailMessage
import noodle.ynabsync.core.port.GmailClient
import noodle.ynabsync.infrastructure.toFinanceDomain

class KtorGmailClientAdapter(httpClient: HttpClient, block: HttpClientConfig<*>.() -> Unit) : KtorGmailClient(httpClient, block), GmailClient {
    override suspend fun getMessage(request: GmailMessageRequest) =
        getMessage(messageId = request.id) {
            parameter("format", request.format.value)
        }.let {
            when (val status = it.status.value) {
                200 -> it.body<GmailMessage>().toFinanceDomain().copy(status = status)
                else -> noodle.ynabsync.core.domain.GmailMessage(status = status)
            }
        }
}
