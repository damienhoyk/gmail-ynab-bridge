package noodle.email

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import noodle.client.Google
import noodle.security.GoogleAuthClient
import noodle.security.*
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

    private val bitwardenAsync = initScope.async { Bitwarden(secretsManagerClientAsync.await()) }

    private val googleSecretAsync = initScope.async { bitwardenAsync.await().getSecret("google")?.jsonObject()!! }
    private val googleAuthClientAsync = initScope.async { GoogleAuthClient() }
    private val googleAsync = initScope.async {
        val secret = googleSecretAsync.await()
        Google(secret.clientId!!, secret.clientSecret!!,
            tokenRepositoryAsync.await(),
            googleAuthClientAsync.await()
        )
    }

    private val tokenRepositoryAsync = initScope.async { TokenRepository(client = dynamoDbClientAsync.await()) }
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

        val google = googleAsync.await()
        val googleGmailClient = google.gmailClient(emailAddress)

        val mailbox = mailboxAsync.await().item().toMutableMap()
        val mailboxState = mailbox["state"]?.n()?.toLong()

        if (mailboxState == null) {
            log.warn("invalid mailbox state")
            return@runBlocking buildJsonObject { put("statusCode", 500) }.toString()
        }

        val historyRequest = GmailHistoryRequest(mailboxState, listOf(historyType))
        val history =  googleGmailClient.getHistory(request = historyRequest).body<GmailHistory>()

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