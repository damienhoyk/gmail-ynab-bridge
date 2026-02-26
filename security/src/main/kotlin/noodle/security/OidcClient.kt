package noodle.security

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

open class OidcClient(
    private val discoveryUrl: String,
    block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {}
) : OAuth2TokenProvider() {

    private val initScope = CoroutineScope(Default)

    override val httpClient = HttpClient(CIO) {

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        block()
    }

    val discoveryDocument = initScope.async {
        getDiscoveryDocument().body<JsonObject>()
    }

    override val tokenEndpoint = initScope.async {
        val discoveryDocument = discoveryDocument.await()
        discoveryDocument["token_endpoint"]?.content ?: throw IllegalStateException()
    }

    val revocationEndpoint = initScope.async {
        val discoveryDocument = discoveryDocument.await()
        discoveryDocument["revocation_endpoint"]?.content ?: throw IllegalStateException()
    }

    val authorizationEndpoint = initScope.async {
        val discoveryDocument = discoveryDocument.await()
        discoveryDocument["authorization_endpoint"]?.jsonPrimitive?.content ?: throw IllegalStateException()
    }

    suspend fun getDiscoveryDocument() = httpClient.get(discoveryUrl)

    suspend fun revokeToken(block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .post(revocationEndpoint.await(), block)

}