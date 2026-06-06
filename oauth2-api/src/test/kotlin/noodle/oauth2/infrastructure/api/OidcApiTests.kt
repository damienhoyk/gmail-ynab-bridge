package noodle.oauth2.infrastructure.api

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
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private val json = Json { ignoreUnknownKeys = true }

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OidcApiTests {
    val discoveryUrl = "https://oauth.example.com/.well-known/openid-configuration"
    val tokenEndpoint = "https://oauth.example.com/token"

    val engine =
        MockEngine {
            val text =
                when (it.method to it.url.encodedPath) {
                    Get to "/.well-known/openid-configuration" ->
                        """
                        {
                          "token_endpoint": "$tokenEndpoint"
                        }
                        """.trimIndent()

                    Post to "/token" ->
                        """
                        {
                          "access_token": "access_123",
                          "id_token": "id_456",
                          "refresh_token": "refresh_789",
                          "expires_in": 3600
                        }
                        """.trimIndent()

                    else -> "{}"
                }

            when (it.method to it.url.encodedPath) {
                Get to "/.well-known/openid-configuration",
                Post to "/token",
                ->
                    respond(
                        content = ByteReadChannel(text),
                        status = OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                else -> respondError(NotFound)
            }
        }

    val httpClient =
        HttpClient(engine) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }

    val oidcApi = OidcApi(discoveryUrl, httpClient)

    @Test
    fun getDiscoveryDocument() =
        runBlocking {
            val doc = oidcApi.getDiscoveryDocument().body<OidcDiscoveryDocument>()
            assertEquals(tokenEndpoint, doc.tokenEndpoint)
        }

    @Test
    fun requestToken() =
        runBlocking {
            val response = oidcApi.requestToken { append("grant_type", "refresh_token") }
            assertNotNull(response)
            val token = response!!.body<OAuth2TokenResponse>()
            assertEquals("access_123", token.accessToken)
            assertEquals("id_456", token.idToken)
            assertEquals("refresh_789", token.refreshToken)
            assertEquals(3600, token.expiresIn)
            assertNull(token.error)
        }

    @Test
    fun tokenResponseError() {
        val raw = """{"error":"invalid_grant"}"""
        val token = json.decodeFromString<OAuth2TokenResponse>(raw)
        assertEquals("invalid_grant", token.error)
        assertNull(token.accessToken)
    }

    @Test
    fun tokenResponsePartial() {
        val raw = """{"access_token":"tok","expires_in":900}"""
        val token = json.decodeFromString<OAuth2TokenResponse>(raw)
        assertEquals("tok", token.accessToken)
        assertEquals(900, token.expiresIn)
        assertNull(token.idToken)
        assertNull(token.refreshToken)
        assertNull(token.error)
    }
}
