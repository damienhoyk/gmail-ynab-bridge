package noodle.event.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import noodle.google.auth.GoogleAuthClient
import noodle.google.event.GmailEvent
import noodle.google.gmail.GoogleGmailClient
import noodle.google.gmail.History
import noodle.google.gmail.HistoryRequest
import noodle.home.security.*
import noodle.email.MailRepository
import noodle.email.MailboxRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.util.Base64.getUrlDecoder

class GmailPubsubHandler : RequestHandler<APIGatewayV2HTTPEvent, String> {

    private val log = LoggerFactory.getLogger(javaClass)
    private val initScope = CoroutineScope(Default)
    private val mapper = Json { ignoreUnknownKeys = true }

    private val credentialsProviderAsync = initScope.async { DefaultCredentialsProvider.create() }

    private val dynamoDbClientAsync = initScope.async {
        DynamoDbClient.builder()
            .credentialsProvider(credentialsProviderAsync.await())
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build()
    }

    private val secretsManagerClientAsync = initScope.async {
        SecretsManagerClient.builder()
            .credentialsProvider(credentialsProviderAsync.await())
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build()
    }

    private val bitwardenSecretAsync = initScope.async(IO) {
        val secretsManagerClient = secretsManagerClientAsync.await()
        secretsManagerClient.getSecret("bitwarden")
    }

    private val bitwardenClientAsync = initScope.async {
        val bitwardenClient = bitwardenClient()
        val bitwardenSecret = bitwardenSecretAsync.await().jsonObject()
        val bitwardenApiKey = bitwardenSecret.clientSecret!!
        bitwardenClient.apply { auth().authorize(bitwardenApiKey) }
    }

    private val tokenStoreAsync = initScope.async { DynamoDbTokenStore(dynamoDbClientAsync.await()) }

    private val googleAuthClientAsync = initScope.async(IO) { GoogleAuthClient() }
    private val googleTokenProviderAsync = initScope.async {
        val bitwardenSecret = bitwardenSecretAsync.await().jsonObject()
        val bitwardenOrganizationId = bitwardenSecret.clientId!!
        val bitwardenClient = bitwardenClientAsync.await()
        val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenClient, bitwardenOrganizationId)
        val tokenStore = tokenStoreAsync.await()
        CachedAccessTokenProvider(googleCredentialsProvider, tokenStore, googleAuthClientAsync.await())
    }

    private val mailboxRepositoryAsync = initScope.async { MailboxRepository(client = dynamoDbClientAsync.await()) }
    private val mailRepositoryAsync = initScope.async { MailRepository(client = dynamoDbClientAsync.await()) }

    private val decoder = getUrlDecoder()

    private val historyType = "messageAdded"

    override fun handleRequest(request: APIGatewayV2HTTPEvent, context: Context?) = runBlocking {
        val headers = request.headers
        val bearerToken = headers?.get("authorization")?.substringAfter("Bearer ")

        val notification = mapper.decodeFromString<PubsubNotification>(request.body)
        val notificationData = decoder.decode(notification.message.data)
        val event = mapper.decodeFromString<GmailEvent>(String(notificationData))
        val eventHistoryId = event.historyId
        val emailAddress = event.emailAddress

        val googleAuthClient = googleAuthClientAsync.await()
        val tokenInfoAsync = async { googleAuthClient.getTokenInfo { parameter("id_token", bearerToken) } }

        val mailboxRepository = mailboxRepositoryAsync.await()
        val mailboxAsync = async { mailboxRepository.get(emailAddress) }

        val googleTokenProvider = googleTokenProviderAsync.await()
        val googleGmailClient = GoogleGmailClient(emailAddress, googleTokenProvider)

        val mailbox = mailboxAsync.await().item().toMutableMap()
        val mailboxState = mailbox["state"]?.n()?.toLong()

        if (mailboxState == null) {
            log.warn("invalid mailbox state")
            return@runBlocking buildJsonObject { put("statusCode", 500) }.toString()
        }

        val historyRequest = HistoryRequest(mailboxState, listOf(historyType))
        val history =  googleGmailClient.getHistory(request = historyRequest).body<History>()

        val tokenInfo = tokenInfoAsync.await()

        if (!tokenInfo.status.isSuccess()) {
            return@runBlocking buildJsonObject { put("statusCode", tokenInfo.status.value) }.toString()
        }

        val mailRepository = mailRepositoryAsync.await()
        val mailJobs = history.messagesAdded.asSequence().map {
            launch { mailRepository.put(emailAddress, it.message.id!!) }
        }

        mailJobs.toList().joinAll()

        mailboxRepository.put(emailAddress) { put("state", fromN(eventHistoryId.toString())) }

        return@runBlocking buildJsonObject { put("statusCode", 201) }.toString()
    }

}