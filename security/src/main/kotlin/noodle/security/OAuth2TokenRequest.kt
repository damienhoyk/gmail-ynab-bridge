package noodle.security

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.http.Parameters

data class OAuth2TokenRequest(
    val code: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val redirectUri: String? = null,
    val refreshToken: String? = null,
    val grantType: String? = "authorization_code"
) {

    val block: HttpRequestBuilder.() -> Unit = {
        setBody(FormDataContent(Parameters.build {
            code?.let { append("code", it) }
            clientId?.let { append("client_id", it) }
            clientSecret?.let { append("client_secret", it) }
            redirectUri?.let { append("redirect_uri", it) }
            refreshToken?.let { append("refresh_token", it) }
            grantType?.let { append("grant_type", it) }
        }))
    }
}
