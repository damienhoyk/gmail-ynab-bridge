import kotlinx.coroutines.runBlocking
import noodle.home.security.*
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import kotlin.test.Test
import kotlin.test.assertEquals

class BitwardenCredentialsProviderTests {

    val secretsManagerClient = SecretsManagerClient.create()

    @Test
    fun test() = runBlocking {
        val bitwardenSecret = secretsManagerClient.getSecret("bitwarden").jsonObject()
        val organizationId = bitwardenSecret.clientId ?: throw IllegalStateException("organization id is null")
        val apiKey = bitwardenSecret.clientSecret ?: throw IllegalStateException("api key is null")

        val client = bitwardenClient()
        client.auth().authorize(apiKey)

        val testCredentialsProvider = BitwardenCredentialsProvider("test", client, organizationId)
        testCredentialsProvider.load()

        assertEquals("testClientId", testCredentialsProvider.getClientId())
        assertEquals("test-client-secret", testCredentialsProvider.getClientSecret())
    }

}