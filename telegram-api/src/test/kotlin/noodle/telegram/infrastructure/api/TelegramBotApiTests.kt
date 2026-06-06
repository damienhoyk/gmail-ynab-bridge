package noodle.telegram.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import noodle.telegram.infrastructure.api.model.TelegramWebhookEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledInNativeImage
import java.util.UUID.randomUUID
import kotlin.reflect.typeOf

@DisabledInNativeImage
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TelegramBotApiTests {
    private val json = Json { ignoreUnknownKeys = true }

    val apiKey = "${randomUUID()}"
    val subdomain = "${randomUUID()}"

    val engine =
        MockEngine {
            val text =
                when (it.method to it.url.encodedPath) {
                    Get to "/bot$apiKey/getMe" ->
                        """
                        {
                            "ok": true,
                        }
                        """.trimIndent()
                    else -> "{}"
                }

            when (it.method to it.url.encodedPath) {
                Get to "/bot$apiKey/getMe" ->
                    respond(
                        content = ByteReadChannel(text),
                        status = OK,
                        headers = headersOf(ContentType, "application/json"),
                    )
                Get to "/bot$apiKey/setWebhook" -> respondOk()
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

    val client =
        TelegramBotApi(HttpClient(engine)) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }

            defaultRequest { url("https://api.telegram.org/bot$apiKey/") }
        }

    @Test
    fun getMe() {
        runBlocking { client.getMe() }
    }

    @Test
    fun setWebhook() {
        runBlocking { client.setWebhook { parameter("url", "https://$subdomain.lambda-url.ap-southeast-1.on.aws/") } }
    }

    @Test
    fun webhookEvent() {
        val raw =
            """
            {
              "message": {
                "text": "Hello",
                "chat": {
                  "id": 123
                },
                "from": {
                  "id": 456
                }
              }
            }
            """.trimIndent()
        val result = json.decodeFromString(serializer(typeOf<TelegramWebhookEvent>()), raw) as TelegramWebhookEvent
        assertEquals("Hello", result.message?.text)
        assertEquals(123L, result.message?.chat?.id)
        assertEquals(456L, result.message?.from?.id)
    }
}
