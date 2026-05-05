package noodle.email.infrastructure.out

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import noodle.chat.infrastructure.toChatDomain
import noodle.email.domain.GmailHistoryRequest
import noodle.email.domain.GmailMessageRequest
import noodle.email.domain.GmailWatchRequest
import noodle.email.infrastructure.serialization.GmailHistory
import noodle.email.infrastructure.serialization.GmailLabel
import noodle.email.infrastructure.serialization.GmailMessage
import noodle.email.infrastructure.serialization.GmailProfile
import noodle.email.infrastructure.serialization.GmailWatch
import noodle.email.infrastructure.toEmailDomain
import noodle.email.port.out.GmailClient
import noodle.finance.infrastructure.toFinanceDomain

open class KtorGmailClient(httpClient: HttpClient, block: HttpClientConfig<*>.() -> Unit = {}) :
    GmailClient, noodle.finance.port.out.GmailClient, noodle.chat.port.out.GmailClient {
    private val httpClient =
        httpClient.config {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }

            defaultRequest {
                contentType(ContentType.Application.Json)
                url("https://gmail.googleapis.com/gmail/v1/users/")
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }

            block()
        }

    suspend fun getHistory(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.get("$user/history", block).body<GmailHistory>()

    override suspend fun getHistory(request: GmailHistoryRequest) =
        getHistory {
            parameter("startHistoryId", request.startHistoryId)
            request.historyTypes.forEach { parameter("historyTypes", it) }
            request.labelId?.let { parameter("labelId", it) }
        }
            .toEmailDomain()

    suspend fun getMessage(
        user: String = "me",
        messageId: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.get("$user/messages/$messageId", block)

    override suspend fun getMessage(request: GmailMessageRequest) =
        getMessage(messageId = request.id) {
            parameter("format", request.format.value)
        }.let {
            when (val status = it.status.value) {
                200 -> it.body<GmailMessage>().toFinanceDomain().copy(status = status)
                else -> noodle.finance.domain.GmailMessage(status = status)
            }
        }

    suspend fun getProfile(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit,
    ) = httpClient.get("$user/profile", block).body<GmailProfile>()

    override suspend fun getProfile() = getProfile {}.toChatDomain()

    suspend fun getLabels(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.get("$user/labels", block).body<GmailLabel.List>()

    override suspend fun getLabelId(labelName: String) =
        getLabels {}
            .labels
            .firstOrNull { it.name.equals(labelName, ignoreCase = true) }
            ?.id

    suspend fun postWatch(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.post("$user/watch", block).body<GmailWatch>()

    override suspend fun postWatch(request: GmailWatchRequest) =
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
        }
            .toChatDomain()

    suspend fun postStop(
        user: String = "me",
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpClient.post("$user/stop", block)
}
