package noodle.email.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import noodle.email.core.port.GmailClient
import noodle.email.infrastructure.serialization.GmailHistory
import noodle.email.infrastructure.toAddedMessageIds

class KtorGmailClientAdapter(httpClient: HttpClient, block: HttpClientConfig<*>.() -> Unit) : KtorGmailClient(httpClient, block), GmailClient {
    override suspend fun getAddedMessageIds(sinceHistoryId: Long) =
        getHistory {
            parameter("startHistoryId", sinceHistoryId)
            parameter("historyTypes", MESSAGE_ADDED)
        }.body<GmailHistory>()
            .toAddedMessageIds()

    private companion object {
        const val MESSAGE_ADDED = "messageAdded"
    }
}
