package noodle.home.security

import com.bitwarden.sdk.BitwardenClient
import kotlinx.serialization.json.jsonPrimitive

class BitwardenCredentialsProvider(
    secretName: String,
    credentialsProvider: CredentialsProvider,
    bitwardenClient: BitwardenClient? = null
) : BitwardenJsonSecretProvider(secretName, credentialsProvider, bitwardenClient), CredentialsProvider {

    override val clientId: String? get() = secretJson?.get("clientId")?.jsonPrimitive?.content
    override val clientSecret: String? get() = secretJson?.get("clientSecret")?.jsonPrimitive?.content

}