package noodle.google.auth.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import noodle.google.auth.infrastructure.api.model.TokenInfoResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledInNativeImage

@DisabledInNativeImage
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoogleOAuth2ApiTests {
    val engine =
        MockEngine {
            val text =
                when (it.method to it.url.encodedPath) {
                    Post to "/tokeninfo" ->
                        """
                        {
                          "email": "user@example.com"
                        }
                        """.trimIndent()

                    else -> "{}"
                }

            when (it.method to it.url.encodedPath) {
                Post to "/tokeninfo" ->
                    respond(
                        content = ByteReadChannel(text),
                        status = OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                else -> respondError(NotFound)
            }
        }

    val client =
        GoogleOAuth2Api(HttpClient(engine)) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }

    @Test
    fun getTokenInfo() =
        runBlocking {
            val response = client.requestTokenInfo {}.body<TokenInfoResponse>()
            assertEquals("user@example.com", response.email)
        }
}
