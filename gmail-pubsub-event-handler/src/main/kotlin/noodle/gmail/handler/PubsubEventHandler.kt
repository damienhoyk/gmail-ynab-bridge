package noodle.gmail.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
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
import noodle.google.auth.GoogleAuthClient
import noodle.google.event.GmailEvent
import noodle.google.gmail.GoogleGmailClient
import noodle.google.gmail.History
import noodle.google.gmail.HistoryRequest
import noodle.home.security.*
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
    private val bitwardenClient = runBlocking { bitwardenClient() }

    private val tokenStore = DynamoDbTokenStore(dynamoDbClient)

    private val googleAuthClient = GoogleAuthClient()
    private val decoder = getUrlDecoder()

    private val mailboxRepository = MailboxRepository(dynamoDbClient)
    private val mailRepository = MailRepository(dynamoDbClient)

    private val historyType = "messageAdded"

    override fun handleRequest(request: APIGatewayV2HTTPEvent, context: Context?) = runBlocking {
        val deferredBitwardenSecret = async(IO) { secretsManagerClient.getSecret("bitwarden") }

        val headers = request.headers
        val bearerToken = headers?.get("authorization")?.substringAfter("Bearer ")

        val deferredTokenInfo = async(IO) { googleAuthClient.getTokenInfo { parameter("id_token", bearerToken) } }

        val notification = mapper.decodeFromString<PubsubNotification>(request.body)
        val notificationData = decoder.decode(notification.message.data)
        val event = mapper.decodeFromString<GmailEvent>(String(notificationData))
        val emailAddress = event.emailAddress

        val deferredMailbox = async(IO) { mailboxRepository.get(emailAddress).item().toMutableMap() }

        val bitwardenSecret = deferredBitwardenSecret.await().jsonObject()
        val bitwardenOrganizationId = bitwardenSecret.clientId
        val bitwardenApiKey = bitwardenSecret.clientSecret

        if (bitwardenApiKey.isNullOrEmpty()) {
            log.warn("invalid bitwarden api key")
            return@runBlocking buildJsonObject { put("statusCode", 500) }.toString()
        }

        if (bitwardenOrganizationId.isNullOrEmpty()) {
            log.warn("invalid bitwarden organization id")
            return@runBlocking buildJsonObject { put("statusCode", 500) }.toString()
        }

        val authorizeBitwarden = launch(IO) { bitwardenClient.auth().authorize(bitwardenApiKey) }

        val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenClient, bitwardenOrganizationId)
        val googleTokenProvider = CachedAccessTokenProvider(googleCredentialsProvider, tokenStore, googleAuthClient)
        val googleGmailClient = GoogleGmailClient(emailAddress, googleTokenProvider)

        val mailbox = deferredMailbox.await().toMutableMap()
        val mailboxState = mailbox["state"]?.n()?.toLong()

        val tokenInfo = deferredTokenInfo.await()

        if (!tokenInfo.status.isSuccess()) {
            return@runBlocking buildJsonObject { put("statusCode", tokenInfo.status.value) }.toString()
        }

        if (mailboxState == null) {
            log.warn("invalid mailbox state")
            return@runBlocking buildJsonObject { put("statusCode", 500) }.toString()
        }

        val historyRequest = HistoryRequest(mailboxState, listOf(historyType))
        val deferredHistory = async(IO) { googleGmailClient.getHistory(request = historyRequest).body<History>() }

        authorizeBitwarden.join()

        val jobs = deferredHistory.await().messagesAdded.asSequence().map {
            mapOf(
                mailRepository.partitionKey to fromS(emailAddress),
                mailRepository.sortKey to fromS(it.message.id)
            )
        }.map { launch { mailRepository.put(it) } }

        mailbox[mailboxRepository.partitionKey] = fromS(emailAddress)
        mailbox["state"] = fromN(event.historyId.toString())

        jobs.toList().joinAll()

        mailboxRepository.put(mailbox)

        return@runBlocking buildJsonObject { put("statusCode", 201) }.toString()
    }

}