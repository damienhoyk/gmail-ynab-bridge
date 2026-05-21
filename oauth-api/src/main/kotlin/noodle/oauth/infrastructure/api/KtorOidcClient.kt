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

open class KtorOidcClient(
    httpClient: HttpClient,
    private val discoveryUrl: String,
    block: HttpClientConfig<*>.() -> Unit = {},
) : KtorOAuth2TokenProvider() {
    private val initScope = CoroutineScope(Dispatchers.Default)

    override val httpClient =
        httpClient.config {
            install(Logging) {
                logger = Logger.Companion.DEFAULT
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

    val discoveryDocument =
        initScope.async {
            getDiscoveryDocument().body<JsonObject>()
        }

    override val tokenEndpoint =
        initScope.async {
            val discoveryDocument = discoveryDocument.await()
            discoveryDocument["token_endpoint"]?.jsonPrimitive?.content ?: throw IllegalStateException()
        }

    val revocationEndpoint =
        initScope.async {
            val discoveryDocument = discoveryDocument.await()
            discoveryDocument["revocation_endpoint"]?.jsonPrimitive?.content ?: throw IllegalStateException()
        }

    val authorizationEndpoint =
        initScope.async {
            val discoveryDocument = discoveryDocument.await()
            discoveryDocument["authorization_endpoint"]?.jsonPrimitive?.content ?: throw IllegalStateException()
        }

    suspend fun getDiscoveryDocument() = httpClient.get(discoveryUrl)

    suspend fun revokeToken(block: HttpRequestBuilder.() -> Unit = {}) =
        httpClient
            .post(revocationEndpoint.await(), block)
}
