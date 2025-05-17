package noodle.home.security

import kotlinx.serialization.json.jsonPrimitive

class BitwardenCredentialsProvider(
    secretName: String,
    credentialsProvider: CredentialsProvider
) : BitwardenJsonSecretProvider(secretName, credentialsProvider), CredentialsProvider {

    override val clientId: String? get() = secretJson?.get("clientId")?.jsonPrimitive?.content
    override val clientSecret: String? get() = secretJson?.get("clientSecret")?.jsonPrimitive?.content

}