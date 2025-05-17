package noodle.google.auth

import io.ktor.client.request.*
import noodle.home.security.OidcClient
import org.slf4j.LoggerFactory

class GoogleAuthClient : OidcClient("https://accounts.google.com/.well-known/openid-configuration") {

    private val log = LoggerFactory.getLogger(javaClass)

    val tokenInfoEndpoint = "https://oauth2.googleapis.com/tokeninfo"

    suspend fun getTokenInfo(block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .post(tokenInfoEndpoint, block)

}
