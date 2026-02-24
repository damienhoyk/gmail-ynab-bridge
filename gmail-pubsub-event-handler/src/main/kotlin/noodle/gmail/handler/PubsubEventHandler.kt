package noodle.gmail.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import noodle.google.auth.GoogleAuthClient
import noodle.google.event.GmailEvent
import noodle.google.gmail.GoogleGmailClient
import noodle.google.gmail.History
import noodle.google.gmail.HistoryRequest
import noodle.google.gmail.Profile
import noodle.home.security.BitwardenCredentialsProvider
import noodle.home.security.CachedAccessTokenProvider
import noodle.home.security.DynamoDbTokenStore
import noodle.home.security.SecretsManagerCredentialsProvider
import noodle.repository.MailRepository
import noodle.repository.MailboxRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.util.Base64.getUrlDecoder

class PubsubEventHandler: RequestHandler<APIGatewayV2HTTPEvent, String> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val mapper = Json { ignoreUnknownKeys = true }

    private val credentialsProvider = EnvironmentVariableCredentialsProvider.create()
    private val dynamoDbClient = DynamoDbClient.builder().credentialsProvider(credentialsProvider).build()
    private val secretsManagerClient = SecretsManagerClient.builder().credentialsProvider(credentialsProvider).build()

    private val tokenStore = DynamoDbTokenStore(dynamoDbClient)

    private val bitwardenCredentialsProvider = SecretsManagerCredentialsProvider("bitwarden", secretsManagerClient)
    private val bitwardenClient = BitwardenClient(BitwardenSettings()).apply {
        auth().loginAccessToken(bitwardenCredentialsProvider.clientSecret, "build/bitwarden-state")
    }

    private val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenCredentialsProvider, bitwardenClient)
    private val googleAuthClient = GoogleAuthClient()
    private val googleTokenProvider = CachedAccessTokenProvider(googleCredentialsProvider, tokenStore, googleAuthClient)
    private val decoder = getUrlDecoder()

    private val mailboxRepository = MailboxRepository(dynamoDbClient)
    private val mailRepository = MailRepository(dynamoDbClient)

    override fun handleRequest(request: APIGatewayV2HTTPEvent, context: Context?) = runBlocking {
        val notification = mapper.decodeFromString<PubsubNotification>(request.body)
        val notificationData = decoder.decode(notification.message.data)
        val event = mapper.decodeFromString<GmailEvent>(String(notificationData))
        val gmail = event.emailAddress

        val googleGmailClient = GoogleGmailClient(gmail, googleTokenProvider)

        val deferredProfile = async(IO) {
            googleGmailClient.getProfile().body<Profile>()
        }

        val headers = request.headers
        val bearerToken = headers?.get("authorization")?.substringAfter("Bearer ")

        val deferredTokenInfo = async(IO) {
            googleAuthClient.getTokenInfo { parameter("id_token", bearerToken) }
        }

        val mailbox = mailboxRepository.get(gmail).item().toMutableMap()
        val mailboxState = mailbox["state"]?.n()?.toLong() ?: deferredProfile.await().historyId

        val historyType = "messageAdded"
        val historyRequest = HistoryRequest(mailboxState, listOf(historyType))
        val deferredHistory = async(IO) {
            googleGmailClient.getHistory(request = historyRequest).body<History>()
        }

        val tokenInfo = deferredTokenInfo.await()

        if (!tokenInfo.status.isSuccess()) {
            return@runBlocking buildJsonObject {
                put("statusCode", tokenInfo.status.value)
                putJsonObject("body") { put("message", tokenInfo.status.description) }
            }.toString()
        }

        val history = deferredHistory.await().messagesAdded

        val jobs = history.asSequence().map {
            mapOf(
                mailRepository.partitionKey to fromS(gmail),
                mailRepository.sortKey to fromS(it.message.id)
            )
        }.map { launch { mailRepository.put(it) } }

        mailbox[mailboxRepository.partitionKey] = fromS(gmail)
        mailbox["state"] = fromN(event.historyId.toString())

        jobs.toList().joinAll()

        mailboxRepository.put(mailbox)

        return@runBlocking buildJsonObject {
            put("statusCode", 200)
            putJsonObject("body") { put("message", "success") }
        }.toString()
    }

}