package noodle.finance.budget.account

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import noodle.google.auth.GoogleAuthClient
import noodle.google.gmail.GoogleGmailClient
import noodle.google.gmail.Label
import noodle.google.gmail.Message
import noodle.home.gmail.ynab.job.TransactionMatcher
import noodle.home.gmail.ynab.job.TransactionMatcher.RegexGroup
import noodle.home.gmail.ynab.job.parse
import noodle.home.security.BitwardenCredentialsProvider
import noodle.home.security.CachedAccessTokenProvider
import noodle.home.security.DynamoDbTokenStore
import noodle.home.security.SecretsManagerCredentialsProvider
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

class Handler : RequestHandler<DynamodbEvent, String> {

    val log = LoggerFactory.getLogger(javaClass)
    val ignoreCase = true

    val credentialsProvider = EnvironmentVariableCredentialsProvider.create()
    val secretsManagerClient = SecretsManagerClient.builder().credentialsProvider(credentialsProvider).build()
    val dynamoDbClient = DynamoDbClient.builder().credentialsProvider(credentialsProvider).build()

    val bitwardenCredentialsProvider = SecretsManagerCredentialsProvider("bitwarden", secretsManagerClient)

    val googleAuthclient = GoogleAuthClient()
    val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenCredentialsProvider)
    val googleTokenProvider = CachedAccessTokenProvider(googleCredentialsProvider, DynamoDbTokenStore(dynamoDbClient), googleAuthclient)

    val matcherTable = "gmail-ynab-bridge-matcher"
    val matchers = dynamoDbClient.scan {
        it.tableName(matcherTable)
    }.items().mapNotNull {
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

    override fun handleRequest(
        event: DynamodbEvent,
        context: Context?
    ) = runBlocking {
        event.records.forEach { record ->
            if (!record.eventName.equals("insert", ignoreCase)) {
                return@forEach
            }

            val gmail = record.dynamodb.newImage["id"]?.s!!
            val googleGmailClient = GoogleGmailClient(gmail, googleTokenProvider)

            val bridge = dynamoDbClient.getItem {
                val key = mapOf("source" to fromS(gmail))
                it.tableName("bridge").key(key)
            }.item()

            val labelName = bridge["label"] ?: "money"
            val labelListResponse = googleGmailClient.getLabels().body<Label.List>()
            val labelId = labelListResponse.labels?.find { it.id == labelName }?.id

            val messagesResponse = googleGmailClient.getMessages {
                labelId?.let { parameter("labelIds", it) }
            }.body<Message.List>()

            val transactions = messagesResponse.messages.map {
                async {
                    val messageResponse = googleGmailClient.getMessage(id = it.id!!)
                    val message = messageResponse.body<Message>().text
                    matchers.parse(message)
                }
            }.awaitAll()

            val accounts = transactions.mapNotNull { it?.accountId }
            log.info("Found accounts: [{}]", accounts.joinToString())
        }.let {
            "ok"
        }

    }

}