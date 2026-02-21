package noodle.finance.budget.bridge

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
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

    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = Json { ignoreUnknownKeys = true }

    private val credentialsProvider = EnvironmentVariableCredentialsProvider.create()
    private val dynamoDbClient = DynamoDbClient.builder().credentialsProvider(credentialsProvider).build()
    private val secretsManagerClient = SecretsManagerClient.builder().credentialsProvider(credentialsProvider).build()

    private val mainTable = "bridge"
    private val matcherTable = "gmail-ynab-bridge-matcher"

    private val tokenStore = DynamoDbTokenStore(dynamoDbClient)

    private val bitwardenCredentialsProvider = SecretsManagerCredentialsProvider("bitwarden", secretsManagerClient)
    private val bitwardenClient = BitwardenClient(BitwardenSettings()).apply {
        auth().loginAccessToken(bitwardenCredentialsProvider.clientSecret, "build/bitwarden-state")
    }

    private val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenCredentialsProvider, bitwardenClient)
    private val googleAuthClient = GoogleAuthClient()
    private val googleTokenProvider = CachedAccessTokenProvider(googleCredentialsProvider, tokenStore, googleAuthClient)

    private val ynabCredentialsProvider = BitwardenCredentialsProvider("ynab", bitwardenCredentialsProvider, bitwardenClient)
    private val ynabAuthClient = YnabAuthClient()
    private val ynabTokenProvider = CachedAccessTokenProvider(ynabCredentialsProvider, tokenStore, ynabAuthClient)

    private val gmailYnabBridgeMatchers = dynamoDbClient.scan {
        it.tableName(matcherTable)
    }.items()

    private val matchers = gmailYnabBridgeMatchers.mapNotNull {
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

        val notification = request.body?.let { mapper.decodeFromString<PubsubNotification>(it) }

        if (notification == null) {
            return@runBlocking lambdaResponse(400, "body is null or empty")
        }

        val notificationData = notification.message.data.let(getUrlDecoder()::decode).let(::String)
        val event = notificationData.let { mapper.decodeFromString<GmailEvent>(it) }
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
                } catch (_: ConditionalCheckFailedException) {
                    log.info("⏸️ Function already running for [$gmail|$ynabId]")
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
                    log.info("⏭️ Skipping job [{}|{}|{} > {}]", gmail, ynabId, lastHistoryId, event.historyId)

                    updateStatus(key, "completed")
                    return@launch
                }

                val currentHistoryId = try {
                    log.info("▶️ Running job [{}|{}|{}] ...", gmail, ynabId, lastHistoryId)
                    job.run(lastHistoryId)
                } catch (e: Exception) {
                    log.warn("🛑 Job run failed [{}]", e.message)

                    updateStatus(key, "failed")
                    return@launch
                }

                log.info("✨️ Job completed [{}]", currentHistoryId)

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

    private fun lambdaResponse(statusCode: Int, message: String? = null) = buildJsonObject {
        put("statusCode", statusCode)
        putJsonObject("body") {
            put("message", message)
        }
    }.toString()

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
