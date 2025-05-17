package noodle.home.security

import kotlinx.serialization.json.jsonPrimitive

class BitwardenApiKeyProvider(
    secretName: String,
    credentialsProvider: CredentialsProvider
) : BitwardenJsonSecretProvider(secretName, credentialsProvider), AccessTokenProvider {

    override fun getToken(id: String?): String {
        return secretJson?.get("apiKey")?.jsonPrimitive?.content!!
    }

    override fun getNewToken(id: String?): String {
        load()
        return secretJson?.get("apiKey")?.jsonPrimitive?.content!!
    }

}