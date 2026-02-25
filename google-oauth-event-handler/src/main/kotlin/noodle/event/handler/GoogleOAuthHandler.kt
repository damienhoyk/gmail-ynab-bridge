package noodle.event.handler

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import noodle.google.auth.GoogleAuthClient
import noodle.home.security.TokenResponse

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