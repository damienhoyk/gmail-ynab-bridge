package noodle.ktor

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

public val DefaultJson: Json = Json { ignoreUnknownKeys = true }

public fun HttpClientConfig<*>.defaultLogging(level: LogLevel = LogLevel.INFO): Unit =
    install(Logging) {
        logger = Logger.DEFAULT
        this.level = level
        sanitizeHeader { it == HttpHeaders.Authorization }
    }

public fun HttpClientConfig<*>.defaultJson(json: Json = DefaultJson): Unit =
    install(ContentNegotiation) {
        json(json)
    }
