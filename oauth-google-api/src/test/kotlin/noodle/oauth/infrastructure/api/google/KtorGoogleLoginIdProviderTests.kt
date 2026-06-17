package noodle.oauth.infrastructure.api.google

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import noodle.google.auth.infrastructure.api.GoogleOAuth2Api
import noodle.oauth.core.domain.TokenResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorGoogleLoginIdProviderTests {
    fun createProvider(tokenInfoJson: String): KtorGoogleLoginIdProvider {
        val engine =
            MockEngine {
                when (it.method to it.url.encodedPath) {
                    Post to "/tokeninfo" ->
                        respond(
                            content = ByteReadChannel(tokenInfoJson),
                            status = OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )

                    else -> respond(content = ByteReadChannel("{}"), status = OK)
                }
            }

        val googleOAuth2Api = GoogleOAuth2Api(HttpClient(engine))
        return KtorGoogleLoginIdProvider(googleOAuth2Api)
    }

    @Test
    fun getLoginIdWithValidSub() =
        runBlocking {
            val provider =
                createProvider(
                    """
                    {
                      "sub": "user123",
                      "email": "user@example.com"
                    }
                    """.trimIndent(),
                )

            val response = TokenResponse(idToken = "fake-id-token")
            val identity = provider.getLoginId(response)

            assertEquals("noodle.oauth://user123@google.com", identity?.id)
            assertEquals(listOf("noodle.oauth://user@example.com"), identity?.aliases)
        }

    @Test
    fun getLoginIdWithNullSub() =
        runBlocking {
            val provider =
                createProvider(
                    """
                    {
                      "email": "user@example.com"
                    }
                    """.trimIndent(),
                )

            val response = TokenResponse(idToken = "fake-id-token")
            val identity = provider.getLoginId(response)

            assertNull(identity)
        }

    @Test
    fun getLoginIdWithNullIdToken() =
        runBlocking {
            val provider =
                createProvider(
                    """
                    {
                      "sub": "user123"
                    }
                    """.trimIndent(),
                )

            val response = TokenResponse(idToken = null)
            val identity = provider.getLoginId(response)

            assertNull(identity)
        }

    @Test
    fun getLoginIdWithNullEmail() =
        runBlocking {
            val provider =
                createProvider(
                    """
                    {
                      "sub": "user123"
                    }
                    """.trimIndent(),
                )

            val response = TokenResponse(idToken = "fake-id-token")
            val identity = provider.getLoginId(response)

            assertEquals("noodle.oauth://user123@google.com", identity?.id)
            assertEquals(emptyList<String>(), identity?.aliases)
        }
}
