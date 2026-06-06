package noodle.ynab.auth.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import noodle.oauth2.infrastructure.api.OAuth2TokenResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledInNativeImage

@DisabledInNativeImage
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
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
    fun requestToken() =
        runBlocking {
            val result =
                client
                    .requestToken {
                        append("grant_type", "authorization_code")
                        append("code", "xyz")
                    }.body<OAuth2TokenResponse>()
            assertEquals("abc", result.accessToken)
            assertEquals(3600, result.expiresIn)
            assertNull(result.idToken)
            assertNull(result.refreshToken)
            assertNull(result.error)
            val request = engine.requestHistory.last()
            assertEquals(
                ContentType.Application.FormUrlEncoded,
                request.body.contentType?.withoutParameters(),
            )
            assertEquals(
                ContentType.Application.Json.toString(),
                request.headers[HttpHeaders.Accept],
            )
        }
}
