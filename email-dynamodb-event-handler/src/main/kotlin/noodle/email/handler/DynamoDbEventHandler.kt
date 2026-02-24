package noodle.email.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings
import io.ktor.client.call.*
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import noodle.google.auth.GoogleAuthClient
import noodle.google.gmail.GoogleGmailClient
import noodle.google.gmail.Message
import noodle.google.gmail.MessageRequest
import noodle.google.gmail.MessageRequest.Format
import noodle.home.gmail.ynab.job.TransactionMatcher
import noodle.home.gmail.ynab.job.TransactionMatcher.RegexGroup
import noodle.home.security.BitwardenCredentialsProvider
import noodle.home.security.CachedAccessTokenProvider
import noodle.home.security.DynamoDbTokenStore
import noodle.home.security.SecretsManagerCredentialsProvider
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

class DynamoDbEventHandler: RequestHandler<DynamodbEvent, String> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val credentialsProvider = EnvironmentVariableCredentialsProvider.create()
    private val dynamoDbClient = DynamoDbClient.builder().credentialsProvider(credentialsProvider).build()
    private val secretsManagerClient = SecretsManagerClient.builder().credentialsProvider(credentialsProvider).build()

    private val tokenStore = DynamoDbTokenStore(dynamoDbClient)

    private val bitwardenCredentialsProvider = SecretsManagerCredentialsProvider("bitwarden", secretsManagerClient)
    private val bitwardenClient = BitwardenClient(BitwardenSettings()).apply {
        auth().loginAccessToken(bitwardenCredentialsProvider.clientSecret, "build/bitwarden-state")
    }
    private val ynabCredentialsProvider = BitwardenCredentialsProvider("ynab", bitwardenCredentialsProvider, bitwardenClient)
    private val ynabAuthClient = YnabAuthClient()
    private val ynabTokenProvider = CachedAccessTokenProvider(ynabCredentialsProvider, tokenStore, ynabAuthClient)

    private val bridgeRepository = BridgeRepository(client = dynamoDbClient)
    private val matcherRepository = MatcherRepository(dynamoDbClient)

    override fun handleRequest(request: DynamodbEvent, context: Context?) = runBlocking {
        request.records.flatMap { record ->
            if (!"insert".equals(record.eventName, ignoreCase = true)) {
                return@flatMap emptyList()
            }

            val mail = record.dynamodb.newImage.toMutableMap()
            val mailAddress = mail["address"]?.s
            val mailId = mail["mailId"]?.s

            if (mailId.isNullOrEmpty()) {
                log.info("Invalid mailId")
                return@flatMap emptyList()
            }

            if (mailAddress.isNullOrEmpty()) {
                log.info("Invalid mailAddress")
                return@flatMap emptyList()
            }

            val (fromAddress, messageText) = when {
                mailAddress.endsWith("gmail.com", ignoreCase = true) -> getGmail(mailId, mailAddress)
                else -> return@flatMap emptyList()
            }

            val bridges = bridgeRepository.get(mailAddress).items()
            val bridgeDestinations = bridges.mapNotNull { it["destination"]?.s() }
            val bridgeDestinationClients = bridgeDestinations.map { YnabClient(it, ynabTokenProvider) }
            val bridgeAccounts = bridges.mapNotNull { it["account"]?.m() }

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

            bridgeDestinationClients.zip(bridgeAccounts).map { (ynabClient, bridgeAccounts) ->
                launch {
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

                    ynabClient.postTransactions(request = TransactionsRequest(ynabBody))
                }
            }
        }.joinAll()

        return@runBlocking "OK"
    }

    private suspend fun getGmail(mailId: String, mailAddress: String) = coroutineScope {
        val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenCredentialsProvider, bitwardenClient)
        val googleAuthClient = GoogleAuthClient()
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

}