package noodle.ynab.auth.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.headersOf
import io.ktor.http.parameters
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class YnabAuthApiTests {
    val engine =
        MockEngine {
            val text =
                when (it.method to it.url.encodedPath) {
                    Post to "/oauth/token" ->
                        """
                        {
                            "access_token": "abc",
                            "token_type": "bearer",
                            "expires_in": 3600
                        }
                        """.trimIndent()
                    else -> "{}"
                }

            when (it.method to it.url.encodedPath) {
                Post to "/oauth/token" ->
                    respond(
                        content = ByteReadChannel(text),
                        status = OK,
                        headers = headersOf(ContentType, "application/json"),
                    )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

    val client =
        YnabAuthApi(HttpClient(engine)) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }

    @Test
    fun requestToken() {
        runBlocking {
            val response =
                client.requestToken(
                    parameters {
                        append("grant_type", "authorization_code")
                        append("code", "xyz")
                    },
                )
            assertEquals(OK, response.status)
        }
    }
}
