package noodle.home.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

class SecretsManagerCredentialsProvider(
    private val secretName: String,
    private val secretsManagerClient: SecretsManagerClient = SecretsManagerClient.create(),
    private val idKey: String = "clientId",
    private val secretKey: String = "clientSecret"
) : CredentialsProvider {

    private var secretsJson: JsonObject? = null

    init {
        load()
    }

    override val clientId: String?
        get() = secretsJson?.get(idKey)?.jsonPrimitive?.content

    override val clientSecret: String?
        get() = secretsJson?.get(secretKey)?.jsonPrimitive?.content

    override fun load() {
        secretsJson = secretsManagerClient.getSecretValue {
            it.secretId(secretName)
        }?.let {
            Json.decodeFromString<JsonObject>(it.secretString())
        }
    }

}