package noodle.event.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import io.ktor.client.call.*
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import noodle.google.auth.GoogleAuthClient
import noodle.google.gmail.GoogleGmailClient
import noodle.google.gmail.Message
import noodle.google.gmail.MessageRequest
import noodle.google.gmail.MessageRequest.Format
import noodle.home.gmail.ynab.job.TransactionMatcher
import noodle.home.gmail.ynab.job.TransactionMatcher.RegexGroup
import noodle.home.security.*
import noodle.repository.BridgeRepository
import noodle.repository.MatcherRepository
import noodle.ynab.Transaction
import noodle.ynab.TransactionsRequest
import noodle.ynab.YnabAuthClient
import noodle.ynab.YnabClient
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

class EmailDynamoDbHandler : RequestHandler<DynamodbEvent, String> {

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

    private val bitwardenSecretAsync = initScope.async {
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

    private val ynabAuthClientAsync = initScope.async { YnabAuthClient() }
    private val ynabTokenProviderAsync = initScope.async {
        val bitwardenSecret = bitwardenSecretAsync.await().jsonObject()
        val bitwardenOrganizationId = bitwardenSecret.clientId!!
        val bitwardenClient = bitwardenClientAsync.await()
        val ynabCredentialsProvider = BitwardenCredentialsProvider("ynab", bitwardenClient, bitwardenOrganizationId)
        val tokenStore = tokenStoreAsync.await()
        CachedAccessTokenProvider(ynabCredentialsProvider, tokenStore, ynabAuthClientAsync.await())
    }

    private val bridgeRepositoryAsync = initScope.async { BridgeRepository(dynamoDbClientAsync.await()) }
    private val matcherRepositoryAsync = initScope.async { MatcherRepository(dynamoDbClientAsync.await()) }

    override fun handleRequest(request: DynamodbEvent, context: Context?) = runBlocking {
        request.records.filter { "insert".equals(it.eventName, ignoreCase = true) }.map { record ->
            launch {
                val mail = record.dynamodb.newImage.toMutableMap()
                val mailAddress = mail["address"]?.s
                val mailId = mail["mailId"]?.s

                if (mailId.isNullOrEmpty()) {
                    log.info("Invalid mailId")
                    return@launch
                }

                if (mailAddress.isNullOrEmpty()) {
                    log.info("Invalid mailAddress")
                    return@launch
                }

                val ynabTokenProvider = ynabTokenProviderAsync.await()

                val (fromAddress, messageText) = when {
                    mailAddress.endsWith("gmail.com", ignoreCase = true) -> coroutineScope {
                        val googleTokenProvider = googleTokenProviderAsync.await()
                        val client = GoogleGmailClient(mailAddress, googleTokenProvider)

                        val request = MessageRequest(Format.FULL)
                        val message = client.getMessage(id = mailId, request = request).body<Message>()
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
                val bridgeDestinations = bridges.mapNotNull { it["destination"]?.s() }
                val bridgeDestinationClients = bridgeDestinations.map { YnabClient(it, ynabTokenProvider) }
                val bridgeAccounts = bridges.mapNotNull { it["accounts"]?.m() }

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

                bridgeDestinationClients.zip(bridgeAccounts).forEach { (ynabClient, bridgeAccounts) ->
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
                    val ynabBody = Transaction.Body(transactions = listOf(ynabTransaction))

                    launch(IO) {
                        ynabClient.postTransactions(request = TransactionsRequest(ynabBody))
                    }
                }
            }
        }.joinAll()

        return@runBlocking "OK"
    }

}