package noodle.tokenrefresher.infrastructure.api.google

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import noodle.oauth2.infrastructure.api.OidcApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledInNativeImage

@DisabledInNativeImage
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorGoogleTokenRefresherTests {
    val tokenEndpoint = "https://oauth2.googleapis.com/token"
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
                            "access_token": "new_access",
                            "refresh_token": "new_refresh",
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
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

    val oidcApi = OidcApi("https://accounts.google.com/.well-known/openid-configuration", HttpClient(engine))

    val refresher = KtorGoogleTokenRefresher(oidcApi, "client_id", "client_secret")

    @Test
    fun refresh() =
        runBlocking {
            val result = refresher.refresh("old_refresh_token")
            assertEquals("new_access", result.accessToken)
            assertEquals("new_refresh", result.refreshToken)
            assertEquals(3600, result.expiresIn)
        }
}
