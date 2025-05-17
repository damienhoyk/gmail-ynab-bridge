package noodle.google.gmail

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.ContentType.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import noodle.home.security.AccessTokenProvider

class GoogleGmailClient(block: BearerAuthConfig.() -> Unit = {}) {

    constructor(gmail: String, googleTokenProvider: AccessTokenProvider): this({

        loadTokens {
            val accessToken = googleTokenProvider.getToken(gmail)
            BearerTokens(accessToken, null)
        }

        refreshTokens {
            val accessToken = googleTokenProvider.getNewToken(gmail)
            BearerTokens(accessToken, null)
        }

    })

    private val httpClient = HttpClient(CIO) {
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

        install(Auth) {
            bearer(block)
        }
    }

    suspend fun getProfile(user: String = "me", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("$user/profile", block)

    suspend fun getHistory(user: String = "me", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("$user/history", block)

    suspend fun getHistory(user: String = "me", request: HistoryRequest) = getHistory(user, request.block)

    suspend fun getMessage(user: String = "me", id: String, block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("$user/messages/$id", block)

    suspend fun getMessage(user: String = "me", id: String, request: MessageRequest) =
        getMessage(user, id, request.block)

    suspend fun getMessages(user: String = "me", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("$user/messages", block)

    suspend fun getLabel(user: String = "me", id: String, block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("$user/labels/$id", block)

    suspend fun getLabels(user: String = "me", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .get("$user/labels", block)

    suspend fun postWatch(user: String = "me", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .post("$user/watch", block)

    suspend fun postWatch(user: String = "me", request: WatchRequest) = postWatch(user, request.block)

    suspend fun postStop(user: String = "me", block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .post("$user/stop", block)

}
