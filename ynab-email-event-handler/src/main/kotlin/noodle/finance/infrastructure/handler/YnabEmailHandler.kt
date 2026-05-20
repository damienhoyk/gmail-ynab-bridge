package noodle.finance.infrastructure.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import noodle.bridge.infrastructure.persistence.DynamoDbBridgeRepository
import noodle.bridge.infrastructure.persistence.DynamoDbMatcherRepository
import noodle.email.infrastructure.persistence.DynamoDbOutboxRepository
import noodle.finance.core.domain.SyncYnabCommand
import noodle.finance.core.service.YnabEmailService
import noodle.finance.infrastructure.KtorYnabClientFactory
import noodle.finance.infrastructure.api.KtorGmailClientFactory
import noodle.security.Bitwarden
import noodle.security.core.clientId
import noodle.security.core.clientSecret
import noodle.security.core.jsonObject
import noodle.security.core.service.AuthTokenService
import noodle.security.infrastructure.api.KtorGoogleAuthClient
import noodle.security.infrastructure.api.KtorYnabAuthClient
import noodle.security.infrastructure.persistence.DynamoDbTokenRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

class YnabEmailHandler : RequestHandler<DynamodbEvent, String> {
    private val log = LoggerFactory.getLogger(javaClass)
    private val initScope = CoroutineScope(Dispatchers.Default)

    private val credentialsProviderAsync = initScope.async { DefaultCredentialsProvider.create() }
    private val urlConnectionClient = UrlConnectionHttpClient.builder()

    private val dynamoDbClientAsync =
        initScope.async {
            DynamoDbClient.builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val secretsManagerClientAsync =
        initScope.async {
            SecretsManagerClient.builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val bitwardenAsync = initScope.async { Bitwarden(secretsManagerClientAsync.await()) }

    private val engineAsync = initScope.async { Java.create() }

    private val googleSecretAsync = initScope.async { bitwardenAsync.await().getSecret("google")?.jsonObject()!! }
    private val googleAuthClientAsync = initScope.async { KtorGoogleAuthClient(HttpClient(engineAsync.await())) }
    private val googleAuthTokenService =
        initScope.async {
            val secret = googleSecretAsync.await()
            AuthTokenService(
                clientId = secret.clientId!!,
                clientSecret = secret.clientSecret!!,
                tokenRepository = tokenRepositoryAsync.await(),
                authClient = googleAuthClientAsync.await(),
            )
        }

    private val ynabSecretAsync = initScope.async { bitwardenAsync.await().getSecret("ynab")?.jsonObject()!! }
    private val ynabAuthClientAsync = initScope.async { KtorYnabAuthClient(HttpClient(engineAsync.await())) }
    private val ynabAuthTokenService =
        initScope.async {
            val secret = ynabSecretAsync.await()
            AuthTokenService(
                clientId = secret.clientId!!,
                clientSecret = secret.clientSecret!!,
                tokenRepository = tokenRepositoryAsync.await(),
                authClient = ynabAuthClientAsync.await(),
            )
        }

    private val gmailClientFactory = initScope.async { KtorGmailClientFactory(googleAuthTokenService.await(), engineAsync.await()) }
    private val ynabClientFactory = initScope.async { KtorYnabClientFactory(ynabAuthTokenService.await(), engineAsync.await()) }

    private val bridgeRepositoryAsync = initScope.async { DynamoDbBridgeRepository(client = dynamoDbClientAsync.await()) }
    private val matcherRepositoryAsync = initScope.async { DynamoDbMatcherRepository(client = dynamoDbClientAsync.await()) }
    private val outboxRepositoryAsync = initScope.async { DynamoDbOutboxRepository(client = dynamoDbClientAsync.await()) }
    private val tokenRepositoryAsync = initScope.async { DynamoDbTokenRepository(client = dynamoDbClientAsync.await()) }

    private val service =
        YnabEmailService(
            ynabClientFactory = { ynabClientFactory.await() },
            gmailClientFactory = { gmailClientFactory.await() },
            bridgeRepository = { bridgeRepositoryAsync.await() },
            matcherRepository = { matcherRepositoryAsync.await() },
            outboxRepository = { outboxRepositoryAsync.await() },
        )

    override fun handleRequest(
        request: DynamodbEvent,
        context: Context?,
    ) = runBlocking {
        request.records
            .filter { "insert".equals(it.eventName, ignoreCase = true) }
            .map { launch { handle(it) } }.joinAll()
        return@runBlocking "OK"
    }

    private suspend fun handle(record: DynamodbEvent.DynamodbStreamRecord) {
        val outbox = record.dynamodb.newImage
        val destination = outbox["destination"]?.s
        val source = outbox["source"]?.s

        if (destination.isNullOrEmpty()) {
            log.error("Outbox record has empty destination [{}]", destination)
            return
        }

        if (source.isNullOrEmpty()) {
            log.error("Outbox record has empty source [{}]", source)
            return
        }

        if (!destination.endsWith("@app.ynab.com", ignoreCase = true)) {
            log.debug("Filter destination [{}]", destination)
            return
        }

        val mailId = source.substringBefore(":")
        val mailAddress = source.substringAfter(":")

        if (mailId.isEmpty() || mailAddress.isEmpty()) {
            log.error("Invalid source format [{}]", source)
            return
        }

        val command = SyncYnabCommand(destination, mailId, mailAddress, source)
        service.execute(command)
    }
}
