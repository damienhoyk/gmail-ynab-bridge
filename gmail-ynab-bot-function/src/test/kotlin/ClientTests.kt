import kotlinx.coroutines.runBlocking
import noodle.home.security.*
import noodle.telegram.bot.TelegramBotClient
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

class ClientTests {

    val secretsManagerClient = SecretsManagerClient.create()
    val bitwardenSecret = runBlocking { secretsManagerClient.getSecret("bitwarden") }.jsonObject()
    val bitwardenOrganizationId = bitwardenSecret.clientId!!
    val bitwardenApiKey = bitwardenSecret.clientSecret!!
    val bitwardenClient = runBlocking { bitwardenClient().apply { auth().authorize(bitwardenApiKey) } }
    val telegramApiKeyProvider = BitwardenApiKeyProvider("telegram", bitwardenClient, bitwardenOrganizationId)

    val client = TelegramBotClient(telegramApiKeyProvider)

    @Test
    fun getMe() {
        runBlocking {
            client.getMe()
        }
    }

    @Disabled
    @Test
    fun setWebhook() {
        runBlocking {
            client.setWebhook("https://zpmjoqq5b4i3ohjawgu6jkt76q0dpeoe.lambda-url.ap-southeast-1.on.aws/")
        }
    }

}