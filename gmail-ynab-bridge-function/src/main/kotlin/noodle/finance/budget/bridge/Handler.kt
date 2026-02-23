package noodle.finance.budget.bridge

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
import noodle.home.security.*
import noodle.repository.BridgeRepository
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

    private val bitwardenClient = BitwardenClient(BitwardenSettings())

    val googleAuthClient = GoogleAuthClient()
    val ynabAuthClient = YnabAuthClient()

    private val gmailYnabBridgeMatchers = dynamoDbClient.scan {
        it.tableName(matcherTable)
    }.items()

    private val bridgeRepository = BridgeRepository(client = dynamoDbClient)

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

    override fun handleRequest(request: APIGatewayV2HTTPEvent, context: Context?): String = runBlocking {
        val deferredBitwardenSecret = async(Dispatchers.IO) {
            secretsManagerClient.getClientSecret("bitwarden")?.jsonObject()!!
        }

        val notification = request.body?.let { mapper.decodeFromString<PubsubNotification>(it) }

        if (notification == null) {
            return@runBlocking lambdaResponse(400, "body is null or empty")
        }

        val headers = request.headers
        val bearerToken = headers?.get("authorization")?.substringAfter("Bearer ")

        val deferredTokenInfo = async(Dispatchers.IO) {
            googleAuthClient.getTokenInfo { parameter("id_token", bearerToken) }
        }

        val notificationData = notification.message.data.let(getUrlDecoder()::decode).let(::String)
        val event = notificationData.let { mapper.decodeFromString<GmailEvent>(it) }
        val gmail = event.emailAddress

        val deferredYnabIds = async(Dispatchers.IO) {
            bridgeRepository.queryAttribute(gmail, "destination")
        }

        val bitwardenSecret = deferredBitwardenSecret.await()
        val bitwardenOrganizationId = bitwardenSecret.clientId ?: throw IllegalStateException()
        val bitwardenApiKey = bitwardenSecret.clientSecret ?: throw IllegalStateException()

        val authorizeBitwarden = launch(Dispatchers.IO) {
            bitwardenClient.auth().authorize(bitwardenApiKey)
        }

        val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenClient, bitwardenOrganizationId)
        val googleTokenProvider = CachedAccessTokenProvider(googleCredentialsProvider, tokenStore, googleAuthClient)

        val ynabCredentialsProvider = BitwardenCredentialsProvider("ynab", bitwardenClient, bitwardenOrganizationId)
        val ynabTokenProvider = CachedAccessTokenProvider(ynabCredentialsProvider, tokenStore, ynabAuthClient)

        val response = deferredTokenInfo.await()
        if (!response.status.isSuccess()) {
            return@runBlocking lambdaResponse(response.status.value, response.status.description)
        }

        authorizeBitwarden.join()
        val ynabIds = deferredYnabIds.await()

        ynabIds.forEach { ynabId ->
            launch(Dispatchers.IO) {
                val bridge = try {
                    val response = bridgeRepository.updateStatusMutex(gmail, ynabId, "running")
                    response.attributes()
                } catch (_: ConditionalCheckFailedException) {
                    log.info("⏸️ Function already running for [$gmail|$ynabId]")
                    return@launch
                }

                val lastHistoryId = bridge["historyId"]?.n()?.toLong() ?: 0L
                val accounts = bridge["accounts"]?.m()?.mapValues { it.value.s() } ?: emptyMap()

                if (lastHistoryId > event.historyId) {
                    log.info("⏭️ Skipping job [{}|{}|{} > {}]", gmail, ynabId, lastHistoryId, event.historyId)

                    bridgeRepository.updateStatus(gmail, ynabId, "completed")
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

                val currentHistoryId = try {
                    log.info("▶️ Running job [{}|{}|{}] ...", gmail, ynabId, lastHistoryId)
                    job.run(lastHistoryId)
                } catch (e: Exception) {
                    log.warn("🛑 Job run failed [{}]", e.message)

                    bridgeRepository.updateStatus(gmail, ynabId, "failed")
                    return@launch
                }

                log.info("✨️ Job completed [{}]", currentHistoryId)

                bridgeRepository.updateStatus(gmail, ynabId, "completed")
                bridgeRepository.updateHistoryId(gmail, ynabId, "$currentHistoryId")
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

}
