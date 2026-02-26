package noodle.security

import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import noodle.finance.YnabClient

class YnabOAuthHandler : OAuthHandler(YnabAuthClient()) {

    override fun getAuthority(response: TokenResponse): String? {
        val client = YnabClient(response.accessToken!!)
        val response = runBlocking { client.getUser().body<JsonObject>() }
        val authority = response["data"]?.jsonObject["user"]?.jsonObject["id"]?.jsonPrimitive?.content
        return authority
    }

}