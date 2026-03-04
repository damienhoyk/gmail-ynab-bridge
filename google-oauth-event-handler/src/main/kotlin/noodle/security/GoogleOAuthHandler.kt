package noodle.security

import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class GoogleOAuthHandler : OAuthHandler(GoogleAuthClient()) {

    override fun getAuthority(response: TokenResponse): String? {
        val tokenInfo = runBlocking {
            (client as GoogleAuthClient).getTokenInfo {
                setBody(FormDataContent(Parameters.build {
                    response.idToken?.let { append("id_token", it) }
                }))
            }.body<JsonObject>()
        }

        val authority = tokenInfo["email"]?.jsonPrimitive?.content

        return authority
    }

}