package noodle.home.security

import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.*

open class BitwardenJsonSecretProvider(
    val secretName: String,
    credentialsProvider: CredentialsProvider,
    bitwardenClient: BitwardenClient? = null
) {

    private val organizationId = credentialsProvider.clientId
    private val apiKey = credentialsProvider.clientSecret

    private val client = bitwardenClient ?: BitwardenClient(BitwardenSettings()).apply {
        auth().loginAccessToken(apiKey, "build/bitwarden-state")
    }

    private val secretsClient = client.secrets()

    var secretJson: JsonObject? = null

    init {
        load()
    }


    fun load() {
        val secrets = secretsClient.list(UUID.fromString(organizationId))

        val secretResponse = secrets.data?.find {
            it.key == secretName
        }

        val secret = secretResponse?.let {
            secretsClient.get(it.id)
        }

        val secretValue  = secret?.value

        secretJson  = secretValue?.let {
            Json.decodeFromString<JsonObject>(it)
        }
    }

}