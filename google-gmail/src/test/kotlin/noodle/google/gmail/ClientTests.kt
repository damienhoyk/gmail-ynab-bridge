package noodle.google.gmail

import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import noodle.google.auth.GoogleAuthClient
import noodle.home.security.BitwardenCredentialsProvider
import noodle.home.security.CachedAccessTokenProvider
import noodle.home.security.DynamoDbTokenStore
import noodle.home.security.SecretsManagerCredentialsProvider
import org.junit.jupiter.api.Order
import kotlin.test.Test

class ClientTests {

    val gmail = "damien.hoyk@gmail.com"
    val authClient = GoogleAuthClient()
    val tokenStore = DynamoDbTokenStore()
    val bitwardenCredentialsProvider = SecretsManagerCredentialsProvider("bitwarden")
    val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenCredentialsProvider)
    val googleAccessTokenProvider = CachedAccessTokenProvider(googleCredentialsProvider, tokenStore, authClient)
    val googleGmailClient = GoogleGmailClient(gmail, googleAccessTokenProvider)

    @Test
    fun getHistory() {
        runBlocking {
            val response =
                googleGmailClient.getHistory(request = HistoryRequest(1760000, listOf("messageAdded"), "INBOX"))
            println(response.toString())
        }
    }

    @Test
    fun getMessage() {
        runBlocking {
            val response = googleGmailClient.getMessage(
                id = "19bedb15b6bb6112",
                request = MessageRequest(MessageRequest.Format.FULL)
            )
            println(response.body<Message>().toString())
        }
    }

    @Order(1)
    @Test
    fun postStop() {
        val response = runBlocking {
            googleGmailClient.postStop()
        }

        assert(response.status.isSuccess())
    }

    @Order(2)
    @Test
    fun postWatch() {
        val labelName = "money"
        val labelId = runBlocking {
            googleGmailClient.getLabels().body<Label.List>().labels
                ?.find { it.name == labelName }
                ?.id ?: throw IllegalStateException()
        }

        val topicName = "projects/lexical-cider-458409-d5/topics/gmail"
        val response = runBlocking {
            val request = WatchRequest(topicName, labelId)
            googleGmailClient.postWatch(request = request)
        }

        assert(response.status.isSuccess())
    }

}