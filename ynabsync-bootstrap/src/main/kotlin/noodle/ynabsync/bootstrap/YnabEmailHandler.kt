package noodle.ynabsync.bootstrap

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import io.ktor.client.engine.java.Java
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import noodle.ktor.bearer
import noodle.ynabsync.core.domain.SyncYnabCommand
import noodle.ynabsync.core.service.YnabEmailService
import noodle.ynabsync.infrastructure.api.KtorGmailClientFactory
import noodle.ynabsync.infrastructure.api.KtorYnabClientFactory
import noodle.ynabsync.infrastructure.persistence.DynamoDbAccessTokenRepository
import noodle.ynabsync.infrastructure.persistence.DynamoDbBankAccountRepository
import noodle.ynabsync.infrastructure.persistence.DynamoDbMatcherRepository
import noodle.ynabsync.infrastructure.persistence.DynamoDbOutboxRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI
import java.net.URISyntaxException

public class YnabEmailHandler : RequestHandler<DynamodbEvent, String> {
    private val log = LoggerFactory.getLogger(javaClass)
    private val initScope = CoroutineScope(Dispatchers.Default)

    private val credentialsProviderAsync = initScope.async { DefaultCredentialsProvider.builder().build() }
    private val urlConnectionClient = UrlConnectionHttpClient.builder()

    private val dynamoDbClientAsync =
        initScope.async {
            DynamoDbClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val engineAsync = initScope.async { Java.create() }

    private val gmailClientFactory =
        initScope.async {
            val accessTokenRepository = accessTokenRepositoryAsync.await()
            KtorGmailClientFactory(
                installAuth = { loginId ->
                    bearer(
                        runBlocking {
                            accessTokenRepository.getAccessToken(loginId)
                                ?: error("No access token for loginId=$loginId; tokenrefresher may not have populated row (id=$loginId, type=access)")
                        },
                    )
                },
                engine = engineAsync.await(),
            )
        }
    private val ynabClientFactory =
        initScope.async {
            val accessTokenRepository = accessTokenRepositoryAsync.await()
            KtorYnabClientFactory(
                installAuth = { loginId ->
                    bearer(
                        runBlocking {
                            accessTokenRepository.getAccessToken(loginId)
                                ?: error("No access token for loginId=$loginId; tokenrefresher may not have populated row (id=$loginId, type=access)")
                        },
                    )
                },
                engine = engineAsync.await(),
            )
        }

    private val accountRepositoryAsync = initScope.async { DynamoDbBankAccountRepository(client = dynamoDbClientAsync.await()) }
    private val matcherRepositoryAsync = initScope.async { DynamoDbMatcherRepository(client = dynamoDbClientAsync.await()) }
    private val outboxRepositoryAsync = initScope.async { DynamoDbOutboxRepository(client = dynamoDbClientAsync.await()) }
    private val accessTokenRepositoryAsync =
        initScope.async { DynamoDbAccessTokenRepository(client = dynamoDbClientAsync.await()) }

    private val service =
        YnabEmailService(
            ynabClientFactory = { ynabClientFactory.await() },
            gmailClientFactory = { gmailClientFactory.await() },
            accountRepository = { accountRepositoryAsync.await() },
            matcherRepository = { matcherRepositoryAsync.await() },
            outboxRepository = { outboxRepositoryAsync.await() },
        )

    public override fun handleRequest(
        request: DynamodbEvent,
        context: Context?,
    ): String =
        runBlocking {
            request.records
                .filter { "insert".equals(it.eventName, ignoreCase = true) }
                .map { launch { handle(it) } }
                .joinAll()
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

        try {
            val uri = URI(destination)
            if (uri.scheme?.equals("noodle.ynabsync", ignoreCase = true) != true || uri.host?.equals("app.ynab.com", ignoreCase = true) != true) {
                log.debug("Filter destination [{}]", destination)
                return
            }
        } catch (e: URISyntaxException) {
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
