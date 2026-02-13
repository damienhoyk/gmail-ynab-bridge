package noodle.finance.budget.bridge

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import noodle.google.auth.GoogleAuthClient
import noodle.google.event.GmailEvent
import noodle.google.gmail.GoogleGmailClient
import noodle.home.gmail.ynab.job.GmailYnabJob
import noodle.home.gmail.ynab.job.TransactionMatcher
import noodle.home.gmail.ynab.job.TransactionMatcher.RegexGroup
import noodle.home.security.BitwardenCredentialsProvider
import noodle.home.security.CachedAccessTokenProvider
import noodle.home.security.DynamoDbTokenStore
import noodle.home.security.SecretsManagerCredentialsProvider
import noodle.ynab.YnabAuthClient
import noodle.ynab.YnabClient
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.util.Base64.getUrlDecoder

class Handler : RequestHandler<APIGatewayV2HTTPEvent, String> {

    val log = LoggerFactory.getLogger(javaClass)
    val mapper = jacksonObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false)

    val credentialsProvider = EnvironmentVariableCredentialsProvider.create()
    val dynamoDbClient = DynamoDbClient.builder().credentialsProvider(credentialsProvider).build()
    val secretsManagerClient = SecretsManagerClient.builder().credentialsProvider(credentialsProvider).build()

    val mainTable = "bridge"
    val matcherTable = "gmail-ynab-bridge-matcher"

    val tokenStore = DynamoDbTokenStore(dynamoDbClient)

    val bitwardenCredentialsProvider = SecretsManagerCredentialsProvider("bitwarden", secretsManagerClient)

    val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenCredentialsProvider)
    val googleAuthClient = GoogleAuthClient()
    val googleTokenProvider = CachedAccessTokenProvider(googleCredentialsProvider, tokenStore, googleAuthClient)

    val ynabCredentialsProvider = BitwardenCredentialsProvider("ynab", bitwardenCredentialsProvider)
    val ynabAuthClient = YnabAuthClient()
    val ynabTokenProvider = CachedAccessTokenProvider(ynabCredentialsProvider, tokenStore, ynabAuthClient)

    val gmailYnabBridgeMatchers = dynamoDbClient.scan {
        it.tableName(matcherTable)
    }.items()

    val matchers = gmailYnabBridgeMatchers.mapNotNull {
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

    override fun handleRequest(request: APIGatewayV2HTTPEvent, context: Context?): String? = runBlocking {
        val headers = request.headers
        val bearerToken = headers?.get("authorization")?.substringAfter("Bearer ")

        val response = googleAuthClient.getTokenInfo {
            parameter("id_token", bearerToken)
        }

        if (!response.status.isSuccess()) {
            return@runBlocking lambdaResponse(response.status.value, response.status.description)
        }

        val notification = request.body?.let { mapper.readValue<PubsubNotification>(it) }

        if (notification == null) {
            return@runBlocking lambdaResponse(400, "body is null or empty")
        }

        val event = notification.message.data.let(getUrlDecoder()::decode).let { mapper.readValue<GmailEvent>(it) }
        val gmail = event.emailAddress

        val bridges = dynamoDbClient.query {
            it.tableName(mainTable).keyConditionExpression("#s = :s")
                .expressionAttributeNames(mapOf("#s" to "source"))
                .expressionAttributeValues(mapOf(":s" to fromS(gmail)))
        }.items()

        bridges.forEach { bridge ->
            launch(Dispatchers.IO) {
                val ynabId = bridge["destination"]?.s()
                val key = mapOf("source" to fromS(gmail), "destination" to fromS(ynabId))

                try {
                    dynamoDbClient.updateItem {
                        val names = mapOf("#s" to "status")
                        val values = mapOf(":s" to fromS("running"))

                        it.tableName(mainTable).key(key)
                            .expressionAttributeNames(names)
                            .expressionAttributeValues(values)
                            .conditionExpression("attribute_not_exists(#s) or not #s = :s")
                            .updateExpression("set #s = :s")
                    }
                } catch (e: ConditionalCheckFailedException) {
                    log.info("⏸️ Function already running for [$gmail][$ynabId]")
                    return@launch
                }

                val lastHistoryId = bridge["historyId"]?.n()?.toLong() ?: 0L
                val accounts = bridge["accounts"]?.m()?.mapValues { it.value.s() } ?: emptyMap()

                if (ynabId == null) {
                    updateStatus(key, "failed")
                    return@launch
                }

                val googleGmailClient = GoogleGmailClient(gmail, googleTokenProvider)
                val ynabClient = YnabClient(ynabId, ynabTokenProvider)

                val job = GmailYnabJob(
                    ynabClient = ynabClient,
                    googleGmailClient = googleGmailClient,
                    accounts = accounts,
                    matchers = matchers
                )

                if (lastHistoryId > event.historyId) {
                    log.info("⏭️ Skipping job run")

                    updateStatus(key, "completed")
                    return@launch
                }

                val currentHistoryId = try {
                    log.info("▶️ Running job with start history id [{}] ...", lastHistoryId)
                    job.run(lastHistoryId)
                } catch (e: Exception) {
                    log.warn("🛑 Job run failed")

                    updateStatus(key, "failed")
                    return@launch
                }

                log.info("✨️ Job completed at [{}]", currentHistoryId)

                dynamoDbClient.updateItem {
                    val names = mapOf("#s" to "status", "#h" to "historyId")
                    val values = mapOf(":s" to fromS("completed"), ":h" to fromN("$currentHistoryId"))

                    it.tableName(mainTable).key(key)
                        .expressionAttributeNames(names)
                        .expressionAttributeValues(values)
                        .updateExpression("set #s = :s, #h = :h")
                }
            }
        }

        return@runBlocking lambdaResponse(200, "success")
    }

    private fun lambdaResponse(statusCode: Int, message: String? = null) = mapOf(
        "statusCode" to statusCode,
        "body" to message?.let { mapOf("message" to it) }
    )
        .filterValues { it != null }
        .let(mapper::writeValueAsString)

    private fun updateStatus(key: Map<String, AttributeValue>, status: String) {
        dynamoDbClient.updateItem {
            val names = mapOf("#s" to "status")
            val values = mapOf(":s" to fromS(status))

            it.tableName(mainTable).key(key)
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .updateExpression("set #s = :s")
        }

    }
}
