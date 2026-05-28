package noodle.bitwarden.infrastructure.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

class Bitwarden(
    private val secretsManagerClient: SecretsManagerClient,
) {
    private val initScope = CoroutineScope(Default)

    private val secret = initScope.async { secretsManagerClient.getSecret("bitwarden") }
    private val secretJson = initScope.async { secret.await()!!.bitwardenSecret() }
    private val clientId = initScope.async { secretJson.await().clientId!! }
    private val clientSecret = initScope.async { secretJson.await().clientSecret!! }

    private val client = initScope.async { bitwardenClient().apply { auth().authorize(clientSecret.await()) } }

    suspend fun getSecret(name: String) = client.await().secrets().getSecret(clientId.await(), name)
}
