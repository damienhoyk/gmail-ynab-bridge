import kotlinx.serialization.json.jsonPrimitive
import noodle.home.security.BitwardenCredentialsProvider
import noodle.home.security.SecretsManagerCredentialsProvider
import kotlin.test.Test

class BitwardenCredentialsProviderTests {

    val bitwardenCredentialsProvider = SecretsManagerCredentialsProvider("bitwarden")
    val testCredentialsProvider = BitwardenCredentialsProvider("test", bitwardenCredentialsProvider)

    @Test
    fun test() {
        testCredentialsProvider.load()

        println(testCredentialsProvider.secretJson?.get("someKey")?.jsonPrimitive?.content)
        println(testCredentialsProvider.clientId)
        println(testCredentialsProvider.clientSecret)

    }
}