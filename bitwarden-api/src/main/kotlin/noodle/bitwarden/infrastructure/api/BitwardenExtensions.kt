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

public suspend fun bitwardenClient(): BitwardenClient = withContext(Default) { BitwardenClient(BitwardenSettings()) }

public suspend fun SecretsClient.getSecret(
    organizationId: String,
    name: String,
): String? =
    withContext(IO) {
        val organizationId = fromString(organizationId)

        val secrets = list(organizationId)
        val secretResponse = secrets.data?.find { it.key.equals(name) }
        val secret = secretResponse?.let { get(it.id) }
        val secretValue = secret?.value

        secretValue
    }

public suspend fun AuthClient.authorize(
    apiKey: String,
    stateFile: String = "build/bitwarden-state",
): AuthClient =
    withContext(IO) {
        this@authorize.apply { loginAccessToken(apiKey, stateFile) }
    }

public suspend fun SecretsManagerClient.getSecret(name: String): String = withContext(IO) { getSecretValue { it.secretId(name) }.secretString() }
