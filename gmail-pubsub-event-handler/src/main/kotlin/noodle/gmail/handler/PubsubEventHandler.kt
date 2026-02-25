package noodle.gmail.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
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
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.util.Base64.getUrlDecoder

class PubsubEventHandler: RequestHandler<APIGatewayV2HTTPEvent, String> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val mapper = Json { ignoreUnknownKeys = true }

    private val initScope = CoroutineScope(Default)

    private val credentialsProviderAsync = initScope.async { EnvironmentVariableCredentialsProvider.create() }
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
    private val bitwardenClientAsync = initScope.async { bitwardenClient() }

    private val tokenStoreAsync = initScope.async { DynamoDbTokenStore(dynamoDbClientAsync.await()) }

    private val googleAuthClient = GoogleAuthClient()
    private val decoder = getUrlDecoder()

    private val mailboxRepositoryAsync = initScope.async { MailboxRepository(dynamoDbClientAsync.await()) }
    private val mailRepositoryAsync = initScope.async { MailRepository(dynamoDbClientAsync.await()) }

    private val historyType = "messageAdded"

    override fun handleRequest(request: APIGatewayV2HTTPEvent, context: Context?) = runBlocking {
        val secretsManagerClient = secretsManagerClientAsync.await()
        val bitwardenSecretAsync = async(IO) { secretsManagerClient.getSecret("bitwarden") }

        val headers = request.headers
        val bearerToken = headers?.get("authorization")?.substringAfter("Bearer ")

        val tokenInfoAsync = async(IO) { googleAuthClient.getTokenInfo { parameter("id_token", bearerToken) } }

        val notification = mapper.decodeFromString<PubsubNotification>(request.body)
        val notificationData = decoder.decode(notification.message.data)
        val event = mapper.decodeFromString<GmailEvent>(String(notificationData))
        val emailAddress = event.emailAddress

        val mailboxRepository = mailboxRepositoryAsync.await()
        val mailRepository = mailRepositoryAsync.await()
        val mailboxAsync = async(IO) { mailboxRepository.get(emailAddress).item().toMutableMap() }

        val bitwardenSecret = bitwardenSecretAsync.await().jsonObject()
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

        val bitwardenClient = bitwardenClientAsync.await()
        val authorizeBitwarden = launch(IO) { bitwardenClient.auth().authorize(bitwardenApiKey) }

        val tokenStore = tokenStoreAsync.await()
        val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenClient, bitwardenOrganizationId)
        val googleTokenProvider = CachedAccessTokenProvider(googleCredentialsProvider, tokenStore, googleAuthClient)
        val googleGmailClient = GoogleGmailClient(emailAddress, googleTokenProvider)

        val mailbox = mailboxAsync.await().toMutableMap()
        val mailboxState = mailbox["state"]?.n()?.toLong()

        val tokenInfo = tokenInfoAsync.await()

        if (!tokenInfo.status.isSuccess()) {
            return@runBlocking buildJsonObject { put("statusCode", tokenInfo.status.value) }.toString()
        }

        if (mailboxState == null) {
            log.warn("invalid mailbox state")
            return@runBlocking buildJsonObject { put("statusCode", 500) }.toString()
        }

        val historyRequest = HistoryRequest(mailboxState, listOf(historyType))
        val historyAsync = async(IO) { googleGmailClient.getHistory(request = historyRequest).body<History>() }

        authorizeBitwarden.join()

        val jobs = historyAsync.await().messagesAdded.asSequence().map {
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