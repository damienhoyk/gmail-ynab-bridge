package noodle.home.security

import com.bitwarden.sdk.BitwardenClient
import kotlinx.coroutines.runBlocking

class BitwardenCredentialsProvider(
    val secretName: String,
    private val client: BitwardenClient = runBlocking { bitwardenClient() },
    private val organizationId: String,
) : CredentialsProvider {

    override suspend fun getClientId() = client.secrets().getClientId(organizationId, secretName)
    override suspend fun getClientSecret() = client.secrets().getClientSecret(organizationId, secretName)

    override suspend fun load() { }

}