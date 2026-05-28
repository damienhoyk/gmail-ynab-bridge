package noodle.bitwarden.infrastructure.api

import com.bitwarden.sdk.AuthClient
import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings
import com.bitwarden.sdk.SecretsClient
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.util.UUID.fromString

suspend fun bitwardenClient() = withContext(Default) { BitwardenClient(BitwardenSettings()) }

suspend fun SecretsClient.getSecret(
    organizationId: String,
    name: String,
) = withContext(IO) {
    val organizationId = fromString(organizationId)

    val secrets = list(organizationId)
    val secretResponse = secrets.data?.find { it.key.equals(name) }
    val secret = secretResponse?.let { get(it.id) }
    val secretValue = secret?.value

    secretValue
}

suspend fun AuthClient.authorize(
    apiKey: String,
    stateFile: String = "build/bitwarden-state",
) = withContext(IO) { apply { loginAccessToken(apiKey, stateFile) } }

suspend fun SecretsManagerClient.getSecret(name: String) = withContext(IO) { getSecretValue { it.secretId(name) }.secretString() }
