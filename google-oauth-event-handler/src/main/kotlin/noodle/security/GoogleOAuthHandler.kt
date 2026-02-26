package noodle.security

import io.ktor.client.call.body
import io.ktor.client.request.parameter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class GoogleOAuthHandler : OAuthHandler(GoogleAuthClient()) {

    override fun getAuthority(response: TokenResponse): String? {
        val tokenInfo = runBlocking {
            (client as GoogleAuthClient).getTokenInfo {
                parameter("id_token", response.idToken)
            }.body<JsonObject>()
        }

        val authority = tokenInfo["email"]?.jsonPrimitive?.content

        return authority
    }

}