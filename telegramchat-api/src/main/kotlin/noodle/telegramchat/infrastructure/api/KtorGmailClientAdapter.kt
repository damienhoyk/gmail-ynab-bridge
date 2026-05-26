package noodle.telegramchat.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import noodle.gmail.infrastructure.api.KtorGmailClient
import noodle.gmail.infrastructure.api.model.GmailLabel
import noodle.gmail.infrastructure.api.model.GmailProfile
import noodle.gmail.infrastructure.api.model.GmailWatch
import noodle.gmailsync.core.domain.GmailWatchRequest
import noodle.telegramchat.core.domain.WatchMailboxRequest
import noodle.telegramchat.core.port.GmailClient
import noodle.telegramchat.infrastructure.toChatDomain

class KtorGmailClientAdapter(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit,
) : KtorGmailClient(httpClient, block),
    GmailClient {
    override suspend fun getProfile() = getProfile {}.body<GmailProfile>().toChatDomain()

    override suspend fun getLabelId(labelName: String) =
        getLabels {}
            .body<GmailLabel.List>()
            .labels
            .firstOrNull { it.name.equals(labelName, ignoreCase = true) }
            ?.id

    override suspend fun postWatch(request: WatchMailboxRequest): noodle.telegramchat.core.domain.GmailWatch {
        val gmailWatchRequest =
            GmailWatchRequest(
                topicName = request.topicName,
                labelIds = request.labelIds,
            )
        return postWatch {
            setBody<JsonObject>(
                buildJsonObject {
                    put("topicName", gmailWatchRequest.topicName)
                    put("labelFilterBehaviour", gmailWatchRequest.labelFilterBehaviour.value)
                    putJsonArray("labelIds") {
                        gmailWatchRequest.labelIds.forEach { add(it) }
                    }
                },
            )
        }.body<GmailWatch>().toChatDomain()
    }
}
