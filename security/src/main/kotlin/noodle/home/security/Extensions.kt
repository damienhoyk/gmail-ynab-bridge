package noodle.home.security

import com.bitwarden.sdk.AuthClient
import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings
import com.bitwarden.sdk.SecretsClient
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.util.UUID.fromString

suspend fun bitwardenClient() = withContext(Default) { BitwardenClient(BitwardenSettings()) }

suspend fun SecretsClient.getSecret(organizationId: String, name: String) = withContext(IO) {
    val organizationId = fromString(organizationId)

    val secrets = list(organizationId)
    val secretResponse = secrets.data?.find { it.key.equals(name) }
    val secret = secretResponse?.let { get(it.id) }
    val secretValue = secret?.value

    secretValue
}

suspend fun SecretsClient.getApiKey(organizationId: String, name: String) =
    getSecret(organizationId, name)?.jsonObject()?.apiKey

suspend fun SecretsClient.getClientId(organizationId: String, name: String) =
    getSecret(organizationId, name)?.jsonObject()?.clientId

suspend fun SecretsClient.getClientSecret(organizationId: String, name: String) =
    getSecret(organizationId, name)?.jsonObject()?.clientSecret

suspend fun SecretsManagerClient.getSecret(name: String) =
    coroutineScope { getSecretValue { it.secretId(name) }.secretString() }

suspend fun SecretsManagerClient.getApiKey(name: String) = getSecret(name)?.jsonObject()?.apiKey
suspend fun SecretsManagerClient.getClientId(name: String) = getSecret(name)?.jsonObject()?.clientId
suspend fun SecretsManagerClient.getClientSecret(name: String) = getSecret(name)?.jsonObject()?.clientSecret

suspend fun AuthClient.authorize(apiKey: String, stateFile: String = "build/bitwarden-state") =
    withContext(IO) { apply { loginAccessToken(apiKey, stateFile) } }

fun String.jsonObject() = Json.decodeFromString<JsonObject>(this)

val JsonObject.apiKey get() = get("apiKey")?.content
val JsonObject.clientId get() = get("clientId")?.content
val JsonObject.clientSecret get() = get("clientSecret")?.content

val JsonElement.content get() = jsonPrimitive.content

