package noodle.ynabsync.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import noodle.gmail.infrastructure.api.KtorGmailClient
import noodle.gmail.infrastructure.api.model.GmailMessage
import noodle.ynabsync.core.domain.MailMessageRequest
import noodle.ynabsync.core.port.GmailClient
import noodle.ynabsync.infrastructure.api.toFinanceDomain

class KtorGmailClientAdapter(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit,
) : KtorGmailClient(httpClient, block),
    GmailClient {
    override suspend fun getMessage(request: MailMessageRequest) =
        getMessage(messageId = request.messageId) {
            parameter("format", request.format.value)
        }.let {
            when (val status = it.status.value) {
                200 -> it.body<GmailMessage>().toFinanceDomain().copy(status = status)
                else ->
                    noodle.ynabsync.core.domain
                        .GmailMessage(status = status)
            }
        }
}
