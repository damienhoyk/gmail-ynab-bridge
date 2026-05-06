package noodle.email.infrastructure.out

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import noodle.email.domain.GmailHistoryRequest
import noodle.email.infrastructure.serialization.GmailHistory
import noodle.email.infrastructure.toEmailDomain
import noodle.email.port.out.GmailClient

class KtorGmailClientAdapter(httpClient: HttpClient, block: HttpClientConfig<*>.() -> Unit) : KtorGmailClient(httpClient, block), GmailClient {
    override suspend fun getHistory(request: GmailHistoryRequest) =
        getHistory {
            parameter("startHistoryId", request.startHistoryId)
            request.historyTypes.forEach { parameter("historyTypes", it) }
            request.labelId?.let { parameter("labelId", it) }
        }.body<GmailHistory>()
            .toEmailDomain()
}
