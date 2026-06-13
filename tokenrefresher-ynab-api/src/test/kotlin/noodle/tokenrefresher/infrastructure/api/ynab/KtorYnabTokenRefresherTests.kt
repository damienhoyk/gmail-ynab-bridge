package noodle.tokenrefresher.infrastructure.api.ynab

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import noodle.ynab.auth.infrastructure.api.YnabAuthApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledInNativeImage

@DisabledInNativeImage
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorYnabTokenRefresherTests {
    val engine =
        MockEngine {
            val text =
                when (it.method to it.url.encodedPath) {
                    Post to "/oauth/token" ->
                        """
                        {
                            "access_token": "ynab_new_access",
                            "refresh_token": "ynab_new_refresh",
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

    val ynabAuthApi = YnabAuthApi(HttpClient(engine))

    val refresher = KtorYnabTokenRefresher(ynabAuthApi, "client_id", "client_secret")

    @Test
    fun refresh() =
        runBlocking {
            val result = refresher.refresh("ynab_old_refresh_token")
            assertEquals("ynab_new_access", result.accessToken)
            assertEquals("ynab_new_refresh", result.refreshToken)
            assertEquals(3600, result.expiresIn)
        }
}
