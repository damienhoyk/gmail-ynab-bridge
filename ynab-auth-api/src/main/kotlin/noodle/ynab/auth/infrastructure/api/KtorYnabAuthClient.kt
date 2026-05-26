package noodle.ynab.auth.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json

open class KtorYnabAuthClient(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val initScope = CoroutineScope(Default)

    val tokenEndpoint = initScope.async { "https://app.ynab.com/oauth/token" }

    val httpClient =
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
}
