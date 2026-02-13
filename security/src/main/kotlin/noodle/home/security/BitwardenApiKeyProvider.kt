package noodle.home.security

import com.bitwarden.sdk.BitwardenClient
import kotlinx.serialization.json.jsonPrimitive

class BitwardenApiKeyProvider(
    secretName: String,
    credentialsProvider: CredentialsProvider,
    bitwardenClient: BitwardenClient? = null
) : BitwardenJsonSecretProvider(secretName, credentialsProvider, bitwardenClient), AccessTokenProvider {

    override fun getToken(id: String?): String {
        return secretJson?.get("apiKey")?.jsonPrimitive?.content!!
    }

    override fun getNewToken(id: String?): String {
        load()
        return secretJson?.get("apiKey")?.jsonPrimitive?.content!!
    }

}