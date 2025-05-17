package noodle.home.security

import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.*

open class BitwardenJsonSecretProvider(
    val secretName: String,
    credentialsProvider: CredentialsProvider
) {

    private val clientSettings = BitwardenSettings()
    private val organizationId = credentialsProvider.clientId
    private val apiKey = credentialsProvider.clientSecret

    private val client = BitwardenClient(clientSettings).apply {
        auth().loginAccessToken(apiKey, "build/bitwarden-state")
    }.secrets()

    var secretJson: JsonObject? = null

    init {
        load()
    }


    fun load() {
        val secrets = client.list(UUID.fromString(organizationId))

        val secretResponse = secrets.data?.find {
            it.key == secretName
        }

        val secret = secretResponse?.let {
            client.get(it.id)
        }

        val secretValue  = secret?.value

        secretJson  = secretValue?.let {
            Json.decodeFromString<JsonObject>(it)
        }
    }

}