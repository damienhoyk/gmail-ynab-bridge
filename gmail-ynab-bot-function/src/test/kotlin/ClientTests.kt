import kotlinx.coroutines.runBlocking
import noodle.home.security.BitwardenApiKeyProvider
import noodle.home.security.SecretsManagerCredentialsProvider
import noodle.telegram.bot.TelegramBotClient
import org.junit.jupiter.api.Test

class ClientTests {

    val bitwardenCredentialsProvider = SecretsManagerCredentialsProvider("bitwarden")
    val telegramApiKeyProvider = BitwardenApiKeyProvider("telegram", bitwardenCredentialsProvider)

    val client = TelegramBotClient(telegramApiKeyProvider)

    @Test
    fun getMe() {
        runBlocking {
            client.getMe()
        }
    }

    @Test
    fun setWebhook() {
        runBlocking {
            client.setWebhook("https://zpmjoqq5b4i3ohjawgu6jkt76q0dpeoe.lambda-url.ap-southeast-1.on.aws/")
        }
    }

}