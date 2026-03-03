package noodle.security

import io.ktor.client.HttpClientConfig
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import org.slf4j.LoggerFactory

class GoogleAuthClient(block: HttpClientConfig<*>.() -> Unit = {}) : OidcClient("https://accounts.google.com/.well-known/openid-configuration", block) {

    private val log = LoggerFactory.getLogger(javaClass)

    val tokenInfoEndpoint = "https://oauth2.googleapis.com/tokeninfo"

    suspend fun getTokenInfo(block: HttpRequestBuilder.() -> Unit = {}) = httpClient
        .post(tokenInfoEndpoint, block)

}
