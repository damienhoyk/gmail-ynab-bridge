package noodle.finance

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import noodle.email.BridgeRepository
import noodle.email.GmailMessage
import noodle.email.GmailMessageRequest
import noodle.client.Google
import noodle.client.Ynab
import noodle.email.MailRepository
import noodle.email.MatcherRepository
import noodle.email.TransactionMatcher
import noodle.security.AuthorizationRepository
import noodle.security.Bitwarden
import noodle.security.GoogleAuthClient
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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import kotlin.time.Clock.System.now
import kotlin.time.Duration.Companion.days

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
            authorizationRepositoryAsync.await(),
            googleAuthClientAsync.await()
        )
    }

    private val ynabSecretAsync = initScope.async { bitwardenAsync.await().getSecret("ynab")?.jsonObject()!! }
    private val ynabAuthClientAsync = initScope.async { YnabAuthClient() }
    private val ynabAsync = initScope.async {
        val secret = ynabSecretAsync.await()
        Ynab(secret.clientId!!, secret.clientSecret!!,
            authorizationRepositoryAsync.await(),
            ynabAuthClientAsync.await()
        )
    }

    private val authorizationRepositoryAsync = initScope.async { AuthorizationRepository(dynamoDbClientAsync.await()) }
    private val bridgeRepositoryAsync = initScope.async { BridgeRepository(client = dynamoDbClientAsync.await()) }
    private val mailRepository = initScope.async { MailRepository(client = dynamoDbClientAsync.await()) }
    private val matcherRepositoryAsync = initScope.async { MatcherRepository(client = dynamoDbClientAsync.await()) }

    override fun handleRequest(request: DynamodbEvent, context: Context?) = runBlocking {
        request.records.filter { "insert".equals(it.eventName, ignoreCase = true) }.map { record ->
            launch {
                val mail = record.dynamodb.newImage.toMutableMap()
                val mailAddress = mail["address"]?.s
                val mailId = mail["mailId"]?.s

                if (mailId.isNullOrEmpty()) {
                    log.error("Invalid mailId")
                    return@launch
                }

                if (mailAddress.isNullOrEmpty()) {
                    log.error("Invalid mailAddress")
                    return@launch
                }

                val ynab = ynabAsync.await()

                val messageResponse = when {
                    mailAddress.endsWith("@gmail.com", ignoreCase = true) -> coroutineScope {
                        val google = googleAsync.await()
                        val client = google.gmailClient(mailAddress)
                        val request = GmailMessageRequest(GmailMessageRequest.Format.FULL)
                        client.getMessage(id = mailId, request = request)
                    }

                    else -> throw IllegalStateException("unknown mail provider")
                }

                if (!messageResponse.status.isSuccess()) {
                    log.error("Invalid response")
                    return@launch
                }

                val (fromAddress, messageText) = when {
                    mailAddress.endsWith("@gmail.com", ignoreCase = true) -> coroutineScope {
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
                val bridges = bridgeRepository.query(mailAddress).items()

                val destinations = bridges.mapNotNull { it["destination"]?.s() }
                val destinationIds = destinations.filter { it.endsWith("@app.ynab.com") }
                val clients = destinationIds.map { ynab.client(it) }
                val accounts = bridges.mapNotNull { it["accounts"]?.m() }

                val matcherRepository = matcherRepositoryAsync.await()
                val matchers = matcherRepository.query(fromAddress).items().mapNotNull {
                    val datePattern = it["datePattern"]?.s()
                    val pattern = it["pattern"]?.s()?.toRegex()
                    val order =
                        it["order"]?.l()?.map(AttributeValue::s)?.map(TransactionMatcher.RegexGroup::valueOf)?.toSet()

                    if (datePattern.isNullOrBlank() || pattern == null || order.isNullOrEmpty()) {
                        log.warn("💩 Matcher [${it["source"] ?: "unknown"}] has invalid configuration")
                        null
                    } else {
                        TransactionMatcher(pattern, order = order, inputDatePattern = datePattern)
                    }
                }

                clients.zip(accounts).forEach { (ynabClient, bridgeAccounts) ->
                    val transaction = matchers.firstNotNullOfOrNull { it.parse(messageText) }

                    if (transaction == null) {
                        log.warn("⚠️ Did not extract any transaction from message [{} ...]", messageText.take(50))
                        return@launch
                    }

                    val bankAccount = transaction.accountId
                    val ynabAccount = bridgeAccounts[bankAccount]?.s()

                    if (ynabAccount.isNullOrEmpty()) {
                        log.warn("⚠️ Transaction did not map to any account [{}]", transaction)
                        return@launch
                    }

                    val ynabTransaction = transaction.copy(accountId = ynabAccount)
                    val ynabBody = YnabTransaction.Body(transactions = listOf(ynabTransaction))

                    val ttl = now().plus(3.days).epochSeconds
                    val mailRepository = mailRepository.await()

                    val job1 = launch {
                        mailRepository.update(mailAddress, mailId) { put("ttl", fromN("$ttl")) }
                    }

                    val job2 = launch {
                        ynabClient.postTransactions(request = YnabTransactionsRequest(ynabBody))
                    }

                    listOf(job1, job2).joinAll()
                }
            }
        }.joinAll()

        return@runBlocking "OK"
    }

}