package noodle.ynab

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import noodle.home.security.OAuth2TokenProvider

class YnabAuthClient : OAuth2TokenProvider() {

    private val initScope = CoroutineScope(Default)

    override val tokenEndpoint = initScope.async { "https://app.ynab.com/oauth/token" }

    override val httpClient = HttpClient {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

}