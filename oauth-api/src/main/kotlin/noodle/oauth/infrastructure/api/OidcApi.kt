package noodle.oauth.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

public class OidcApi(
    httpClient: HttpClient,
    private val discoveryUrl: String,
    block: HttpClientConfig<*>.() -> Unit = {},
) : KtorOAuth2TokenProvider() {
    private val initScope = CoroutineScope(Dispatchers.Default)

    public override val httpClient: HttpClient =
        httpClient.config {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }

            block()
        }

    public val discoveryDocument: kotlinx.coroutines.Deferred<JsonObject> =
        initScope.async {
            getDiscoveryDocument().body<JsonObject>()
        }

    public override val tokenEndpoint: kotlinx.coroutines.Deferred<String> =
        initScope.async {
            val discoveryDocument = discoveryDocument.await()
            discoveryDocument["token_endpoint"]?.jsonPrimitive?.content ?: throw IllegalStateException()
        }

    public val revocationEndpoint: kotlinx.coroutines.Deferred<String> =
        initScope.async {
            val discoveryDocument = discoveryDocument.await()
            discoveryDocument["revocation_endpoint"]?.jsonPrimitive?.content ?: throw IllegalStateException()
        }

    public val authorizationEndpoint: kotlinx.coroutines.Deferred<String> =
        initScope.async {
            val discoveryDocument = discoveryDocument.await()
            discoveryDocument["authorization_endpoint"]?.jsonPrimitive?.content ?: throw IllegalStateException()
        }

    public suspend fun getDiscoveryDocument(): io.ktor.client.statement.HttpResponse = httpClient.get(discoveryUrl)

    public suspend fun revokeToken(block: HttpRequestBuilder.() -> Unit = {}): io.ktor.client.statement.HttpResponse =
        httpClient
            .post(revocationEndpoint.await(), block)
}
