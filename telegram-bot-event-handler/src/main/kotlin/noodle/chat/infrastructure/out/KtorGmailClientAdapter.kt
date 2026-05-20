package noodle.chat.infrastructure.out

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import noodle.chat.core.port.GmailClient
import noodle.chat.infrastructure.toChatDomain
import noodle.email.domain.GmailWatchRequest
import noodle.email.infrastructure.out.KtorGmailClient
import noodle.email.infrastructure.serialization.GmailLabel
import noodle.email.infrastructure.serialization.GmailProfile
import noodle.email.infrastructure.serialization.GmailWatch

class KtorGmailClientAdapter(httpClient: HttpClient, block: HttpClientConfig<*>.() -> Unit) : KtorGmailClient(httpClient, block), GmailClient {
    override suspend fun getProfile() = getProfile {}.body<GmailProfile>().toChatDomain()

    override suspend fun getLabelId(labelName: String) =
        getLabels {}.body<GmailLabel.List>()
            .labels
            .firstOrNull { it.name.equals(labelName, ignoreCase = true) }
            ?.id

    override suspend fun postWatch(request: GmailWatchRequest): noodle.chat.core.domain.GmailWatch =
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
