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
import noodle.gmailsync.core.domain.GmailWatchRequest
import noodle.gmailsync.infrastructure.api.KtorGmailClient
import noodle.gmailsync.infrastructure.serialization.GmailLabel
import noodle.gmailsync.infrastructure.serialization.GmailProfile
import noodle.gmailsync.infrastructure.serialization.GmailWatch
import noodle.telegramchat.core.port.GmailClient
import noodle.telegramchat.infrastructure.toChatDomain

class KtorGmailClientAdapter(httpClient: HttpClient, block: HttpClientConfig<*>.() -> Unit) : KtorGmailClient(httpClient, block), GmailClient {
    override suspend fun getProfile() = getProfile {}.body<GmailProfile>().toChatDomain()

    override suspend fun getLabelId(labelName: String) =
        getLabels {}.body<GmailLabel.List>()
            .labels
            .firstOrNull { it.name.equals(labelName, ignoreCase = true) }
            ?.id

    override suspend fun postWatch(request: GmailWatchRequest): noodle.telegramchat.core.domain.GmailWatch =
        postWatch {
            setBody<JsonObject>(
                buildJsonObject {
                    put("topicName", request.topicName)
                    put("labelFilterBehaviour", request.labelFilterBehaviour.value)
                    putJsonArray("labelIds") {
                        request.labelIds.forEach { add(it) }
                    }
                },
            )
        }.body<GmailWatch>().toChatDomain()
}
