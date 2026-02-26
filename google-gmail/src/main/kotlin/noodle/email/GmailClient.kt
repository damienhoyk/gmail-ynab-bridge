package noodle.email

import io.ktor.client.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.ContentType.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class GmailClient(engine: HttpClientEngine = CIO.create(), block: HttpClientConfig<*>.() -> Unit = {}) {

    private val httpClient = HttpClient(engine) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        defaultRequest {
            contentType(Application.Json)
            url("https://gmail.googleapis.com/gmail/v1/users/")
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }

        block()
    }

    suspend fun getProfile(user: String = "me", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("$user/profile", block)

    suspend fun getHistory(user: String = "me", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("$user/history", block)

    suspend fun getHistory(user: String = "me", request: GmailHistoryRequest) = getHistory(user, request.block)

    suspend fun getMessage(user: String = "me", id: String, block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("$user/messages/$id", block)

    suspend fun getMessage(user: String = "me", id: String, request: GmailMessageRequest) =
        getMessage(user, id, request.block)

    suspend fun getMessages(user: String = "me", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("$user/messages", block)

    suspend fun getLabel(user: String = "me", id: String, block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("$user/labels/$id", block)

    suspend fun getLabels(user: String = "me", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("$user/labels", block)

    suspend fun postWatch(user: String = "me", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .post("$user/watch", block)

    suspend fun postWatch(user: String = "me", request: GmailWatchRequest) = postWatch(user, request.block)

    suspend fun postStop(user: String = "me", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .post("$user/stop", block)

}
