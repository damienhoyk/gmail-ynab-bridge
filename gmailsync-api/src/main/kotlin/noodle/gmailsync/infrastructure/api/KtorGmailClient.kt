package noodle.gmailsync.infrastructure.api

import io.ktor.client.call.body
import io.ktor.client.request.parameter
import noodle.gmail.infrastructure.api.GmailApi
import noodle.gmail.infrastructure.api.model.GmailHistory
import noodle.gmailsync.core.port.GmailClient

class KtorGmailClient(
    private val gmailApi: GmailApi,
) : GmailClient {
    override suspend fun getAddedMessageIds(sinceHistoryId: Long) =
        gmailApi
            .getHistory {
                parameter("startHistoryId", sinceHistoryId)
                parameter("historyTypes", MESSAGE_ADDED)
            }.body<GmailHistory>()
            .toAddedMessageIds()

    private companion object {
        const val MESSAGE_ADDED = "messageAdded"
    }
}

private fun GmailHistory.toAddedMessageIds(): List<String> = history.flatMap { it.messagesAdded }.mapNotNull { it.message.id }
