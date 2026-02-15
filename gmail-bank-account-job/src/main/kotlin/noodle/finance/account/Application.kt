package noodle.finance.account

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
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
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

class Application

fun main(vararg args: String): Unit = runBlocking {
    val gmail = args[0]
    val log = LoggerFactory.getLogger(Application::class.java)

    val credentialsProvider = ContainerCredentialsProvider.create()
    val secretsManagerClient = SecretsManagerClient.builder().credentialsProvider(credentialsProvider).build()
    val dynamoDbClient = DynamoDbClient.builder().credentialsProvider(credentialsProvider).build()

    val bitwardenCredentialsProvider = SecretsManagerCredentialsProvider("bitwarden", secretsManagerClient)

    val googleAuthClient = GoogleAuthClient()
    val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenCredentialsProvider)
    val googleTokenProvider =
        CachedAccessTokenProvider(googleCredentialsProvider, DynamoDbTokenStore(dynamoDbClient), googleAuthClient)

    val mainTable = "bridge"

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

    val googleGmailClient = GoogleGmailClient(gmail, googleTokenProvider)

    val bridges = dynamoDbClient.query {
        it.tableName(mainTable).keyConditionExpression("#s = :s")
            .expressionAttributeNames(mapOf("#s" to "source"))
            .expressionAttributeValues(mapOf(":s" to fromS(gmail)))
    }.items()

    val labelNames = bridges.mapNotNull { it["label"]?.s() }.toSet() + "money"
    val labelListResponse = googleGmailClient.getLabels().body<Label.List>()
    val labelIds = labelNames.map { labelName ->
        labelListResponse.labels?.find { it.name == labelName }?.id
    }

    val messagesResponse = googleGmailClient.getMessages {
        labelIds.forEach { parameter("labelIds", it) }
    }.body<Message.List>()

    val transactions = messagesResponse.messages.map {
        async(Dispatchers.IO) {
            val messageResponse = googleGmailClient.getMessage(id = it.id!!)
            val message = messageResponse.body<Message>().text

            matchers.parse(message)?.accountId
        }
    }.awaitAll().toSet().filterNotNull()
    log.info("found accounts [{}]", transactions.joinToString())
}