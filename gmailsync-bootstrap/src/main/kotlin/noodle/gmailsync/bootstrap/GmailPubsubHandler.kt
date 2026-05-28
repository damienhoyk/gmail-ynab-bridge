package noodle.gmailsync.bootstrap

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import noodle.bitwarden.Bitwarden
import noodle.gmail.infrastructure.api.model.GmailEvent
import noodle.gmail.infrastructure.api.model.pubsub.PubsubNotification
import noodle.gmailsync.core.domain.SyncMailboxCommand
import noodle.gmailsync.core.service.GmailPubsubService
import noodle.gmailsync.infrastructure.api.KtorGmailClientFactory
import noodle.gmailsync.infrastructure.api.KtorGoogleOAuth2Client
import noodle.gmailsync.infrastructure.persistence.DynamoDbBridgeRepository
import noodle.gmailsync.infrastructure.persistence.DynamoDbMailboxRepository
import noodle.gmailsync.infrastructure.persistence.DynamoDbOutboxRepository
import noodle.oauth.core.service.TokenService
import noodle.oauth.infrastructure.api.bearer
import noodle.oauth.infrastructure.api.google.KtorGoogleOidcClient
import noodle.oauth.infrastructure.persistence.DynamoDbTokenRepository
import noodle.serialization.clientId
import noodle.serialization.clientSecret
import noodle.serialization.jsonObject
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.util.Base64

class GmailPubsubHandler : RequestHandler<APIGatewayV2HTTPEvent, String> {
    private val log = LoggerFactory.getLogger(javaClass)
    private val initScope = CoroutineScope(Dispatchers.Default)
    private val decoder = Base64.getUrlDecoder()
    private val mapper = Json { ignoreUnknownKeys = true }

    private val credentialsProviderAsync = initScope.async { DefaultCredentialsProvider.create() }
    private val urlConnectionClient = UrlConnectionHttpClient.builder()

    private val dynamoDbClientAsync =
        initScope.async {
            DynamoDbClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val secretsManagerClientAsync =
        initScope.async {
            SecretsManagerClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val bitwardenAsync = initScope.async { Bitwarden(secretsManagerClientAsync.await()) }

    private val engineAsync = initScope.async { Java.create() }

    private val googleSecretAsync =
        initScope.async { bitwardenAsync.await().getSecret("google")?.jsonObject()!! }
    private val googleOidcClientAsync = initScope.async { KtorGoogleOidcClient(HttpClient(engineAsync.await())) }
    private val googleOAuth2ClientAsync = initScope.async { KtorGoogleOAuth2Client(HttpClient(engineAsync.await())) }
    private val googleAuthTokenService =
        initScope.async {
            val secret = googleSecretAsync.await()
            TokenService(
                clientId = secret.clientId!!,
                clientSecret = secret.clientSecret!!,
                tokenRepository = tokenRepositoryAsync.await(),
                authClient = googleOidcClientAsync.await(),
            )
        }

    private val gmailClientFactory =
        initScope.async {
            val tokenService = googleAuthTokenService.await()
            KtorGmailClientFactory(
                installAuth = { loginId -> bearer(tokenService, loginId) },
                engine = engineAsync.await(),
            )
        }

    private val bridgeRepositoryAsync =
        initScope.async { DynamoDbBridgeRepository(client = dynamoDbClientAsync.await()) }
    private val mailboxRepositoryAsync =
        initScope.async { DynamoDbMailboxRepository(client = dynamoDbClientAsync.await()) }
    private val outboxRepositoryAsync =
        initScope.async { DynamoDbOutboxRepository(client = dynamoDbClientAsync.await()) }
    private val tokenRepositoryAsync =
        initScope.async { DynamoDbTokenRepository(client = dynamoDbClientAsync.await()) }

    private val service =
        GmailPubsubService(
            gmailClientFactory = { gmailClientFactory.await() },
            googleTokenClient = { googleOAuth2ClientAsync.await() },
            mailboxRepository = { mailboxRepositoryAsync.await() },
            bridgeRepository = { bridgeRepositoryAsync.await() },
            outboxRepository = { outboxRepositoryAsync.await() },
        )

    override fun handleRequest(
        request: APIGatewayV2HTTPEvent,
        context: Context?,
    ) = runBlocking {
        val notification = mapper.decodeFromString<PubsubNotification>(request.body)
        val notificationData = decoder.decode(notification.message.data)
        val event = mapper.decodeFromString<GmailEvent>(String(notificationData))

        val command =
            SyncMailboxCommand(
                email = event.emailAddress,
                authorization = request.headers?.get("authorization"),
                state = event.historyId,
            )

        val statusCode = service.execute(command)

        buildJsonObject { put("statusCode", statusCode) }.toString()
    }
}
