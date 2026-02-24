package noodle.email.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import io.ktor.client.call.*
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

class DynamoDbEventHandler : RequestHandler<DynamodbEvent, String> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val credentialsProvider = EnvironmentVariableCredentialsProvider.create()
    private val dynamoDbClient = DynamoDbClient.builder().credentialsProvider(credentialsProvider).build()
    private val secretsManagerClient = SecretsManagerClient.builder().credentialsProvider(credentialsProvider).build()

    private val tokenStore = DynamoDbTokenStore(dynamoDbClient)

    private val bridgeRepository = BridgeRepository(dynamoDbClient)
    private val matcherRepository = MatcherRepository(dynamoDbClient)
    private val bitwardenClient = runBlocking { bitwardenClient() }

    private val googleAuthClient = GoogleAuthClient()
    private val ynabAuthClient = YnabAuthClient()

    override fun handleRequest(request: DynamodbEvent, context: Context?) = runBlocking {
        val deferredBitwardenSecret = async(IO) { secretsManagerClient.getSecret("bitwarden") }

        val bitwardenSecret = deferredBitwardenSecret.await().jsonObject()
        val bitwardenApiKey = bitwardenSecret.clientSecret
        val bitwardenOrganizationId = bitwardenSecret.clientId

        if (bitwardenApiKey.isNullOrEmpty()) {
            log.warn("invalid bitwarden api key")
            return@runBlocking buildJsonObject { put("statusCode", 500) }.toString()
        }

        if (bitwardenOrganizationId.isNullOrEmpty()) {
            log.warn("invalid bitwarden organization id")
            return@runBlocking buildJsonObject { put("statusCode", 500) }.toString()
        }

        bitwardenClient.auth().authorize(bitwardenApiKey)

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

                val ynabCredentialsProvider = BitwardenCredentialsProvider("ynab", bitwardenClient, bitwardenOrganizationId)
                val ynabTokenProvider = CachedAccessTokenProvider(ynabCredentialsProvider, tokenStore, ynabAuthClient)

                val (fromAddress, messageText) = when {
                    mailAddress.endsWith("gmail.com", ignoreCase = true) -> coroutineScope {
                        val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenClient, bitwardenOrganizationId)
                        val googleTokenProvider = CachedAccessTokenProvider(googleCredentialsProvider, tokenStore, googleAuthClient)
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

                val bridges = bridgeRepository.query(mailAddress).items()
                val bridgeDestinations = bridges.mapNotNull { it["destination"]?.s() }
                val bridgeDestinationClients = bridgeDestinations.map { YnabClient(it, ynabTokenProvider) }
                val bridgeAccounts = bridges.mapNotNull { it["accounts"]?.m() }

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