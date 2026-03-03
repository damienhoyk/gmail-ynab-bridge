package noodle.finance

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import noodle.email.BridgeRepository
import noodle.email.GmailMessage
import noodle.email.GmailMessageRequest
import noodle.client.Google
import noodle.client.Ynab
import noodle.email.GmailMessageRequest.Format
import noodle.email.MatcherRepository
import noodle.email.OutboxRepository
import noodle.email.TransactionMatcher
import noodle.email.TransactionMatcher.RegexGroup
import noodle.security.Bitwarden
import noodle.security.GoogleAuthClient
import noodle.security.TokenRepository
import noodle.security.YnabAuthClient
import noodle.security.clientId
import noodle.security.clientSecret
import noodle.security.jsonObject
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import kotlin.time.Clock.System.now
import kotlin.time.Duration.Companion.hours

class YnabEmailHandler : RequestHandler<DynamodbEvent, String> {

    private val log = LoggerFactory.getLogger(javaClass)
    private val initScope = CoroutineScope(Default)

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

    private val ynabSecretAsync = initScope.async { bitwardenAsync.await().getSecret("ynab")?.jsonObject()!! }
    private val ynabAuthClientAsync = initScope.async { YnabAuthClient() }
    private val ynabAsync = initScope.async {
        val secret = ynabSecretAsync.await()
        Ynab(secret.clientId!!, secret.clientSecret!!,
            tokenRepositoryAsync.await(),
            ynabAuthClientAsync.await()
        )
    }

    private val tokenRepositoryAsync = initScope.async { TokenRepository(client = dynamoDbClientAsync.await()) }
    private val bridgeRepositoryAsync = initScope.async { BridgeRepository(client = dynamoDbClientAsync.await()) }
    private val matcherRepositoryAsync = initScope.async { MatcherRepository(client = dynamoDbClientAsync.await()) }
    private val outboxRepositoryAsync = initScope.async { OutboxRepository(client = dynamoDbClientAsync.await()) }

    override fun handleRequest(request: DynamodbEvent, context: Context?) = runBlocking {
        request.records
            .filter { "insert".equals(it.eventName, ignoreCase = true) }
            .map { launch { handle(it) } }.joinAll()
        return@runBlocking "OK"
    }

    private suspend fun handle(record: DynamodbStreamRecord) {
        val outbox = record.dynamodb.newImage.toMutableMap()

        val destination = outbox["destination"]?.s
        val source = outbox["source"]?.s

        if (destination.isNullOrEmpty()) {
            log.error("Invalid destination")
            return
        }

        if (!destination.endsWith("@app.ynab.com", ignoreCase = true)) {
            log.info("Filter destination [{}]", destination)
            return
        }

        if (source.isNullOrEmpty()) {
            log.error("Invalid source")
            return
        }

        val (mailId, mailAddress) = source.split(":")

        if (mailId.isEmpty()) {
            log.error("Invalid mailId")
            return
        }

        if (mailAddress.isEmpty()) {
            log.error("Invalid mailAddress")
            return
        }

        val ynab = ynabAsync.await()

        val messageResponse = when {
            mailAddress.endsWith("@gmail.com", ignoreCase = true) -> {
                val google = googleAsync.await()
                val client = google.gmailClient(mailAddress)
                val request = GmailMessageRequest(Format.FULL)
                client.getMessage(id = mailId, request = request)
            }
            else -> throw IllegalStateException("unknown mail provider")
        }

        when (messageResponse.status.value) {
            403, 404, 410
                -> outboxRepositoryAsync.await().update(destination, source) {
                put("ttl", fromN("${(now() + 1.hours).epochSeconds}"))
            }
            else -> outboxRepositoryAsync.await().update(destination, source) {
                put("ttl", fromN("${(now() + 120.hours).epochSeconds}"))
            }
        }

        if (!messageResponse.status.isSuccess()) {
            log.error("Invalid response")
            return
        }

        val (fromAddress, messageText) = when {
            mailAddress.endsWith("@gmail.com", ignoreCase = true) -> {
                val message = messageResponse.body<GmailMessage>()
                val messageText = message.text
                val messageHeaders = message.payload?.headers

                val fromHeader = messageHeaders?.find { it["name"].equals("from", true) }
                val fromValue = fromHeader?.get("value")

                val fromAddress = InternetAddress(fromValue).address

                fromAddress to messageText
            }
            else -> throw IllegalStateException("unknown mail provider")
        }

        val bridgeRepository = bridgeRepositoryAsync.await()
        val bridge = bridgeRepository.get(mailAddress, destination).item()

        val client = ynab.client(destination)
        val accounts = bridge["accounts"]?.m() ?: emptyMap()

        val matcherRepository = matcherRepositoryAsync.await()
        val matchers = matcherRepository.query(fromAddress).items().mapNotNull {
            val datePattern = it["datePattern"]?.s()
            val pattern = it["pattern"]?.s()?.toRegex()
            val order = it["order"]?.l()?.map(AttributeValue::s)?.map(RegexGroup::valueOf)?.toSet()

            if (datePattern.isNullOrBlank() || pattern == null || order.isNullOrEmpty()) {
                log.warn("💩 Matcher [${it["source"] ?: "unknown"}] has invalid configuration")
                null
            } else {
                TransactionMatcher(pattern, order = order, inputDatePattern = datePattern)
            }
        }

        val transaction = matchers.firstNotNullOfOrNull { it.parse(messageText) }

        if (transaction == null) {
            log.warn("⚠️ Did not extract any transaction from message [{}|{} ...]", mailId, messageText.take(50))
            outboxRepositoryAsync.await().update(destination, source) {
                put("ttl", fromN("${(now() + 1.hours).epochSeconds}"))
            }
            return
        }

        val ynabAccount = accounts[transaction.accountId]?.s()
        val ynabTransaction = transaction.copy(accountId = ynabAccount)
        val ynabBody = YnabTransaction.Body(transactions = listOf(ynabTransaction))

        client.postTransactions(request = YnabTransactionsRequest(ynabBody))

        outboxRepositoryAsync.await().update(destination, source) {
            put("ttl", fromN("${(now() + 24.hours).epochSeconds}"))
        }
    }
}