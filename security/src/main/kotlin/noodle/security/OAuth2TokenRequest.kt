package noodle.security

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter

data class OAuth2TokenRequest(
    val code: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val redirectUri: String? = null,
    val refreshToken: String? = null,
    val grantType: String? = "authorization_code"
) {

    val block: HttpRequestBuilder.() -> Unit = {
        code?.let { parameter("code", it) }
        clientId?.let { parameter("client_id", it) }
        clientSecret?.let { parameter("client_secret", it) }
        redirectUri?.let { parameter("redirect_uri", it) }
        refreshToken?.let { parameter("refresh_token", it) }
        grantType?.let { parameter("grant_type", it) }
    }
}
