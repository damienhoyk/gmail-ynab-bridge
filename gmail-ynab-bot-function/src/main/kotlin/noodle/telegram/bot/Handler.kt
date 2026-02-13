package noodle.telegram.bot

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import noodle.google.auth.GoogleAuthClient
import noodle.google.gmail.GoogleGmailClient
import noodle.google.gmail.Label
import noodle.google.gmail.WatchRequest
import noodle.home.security.*
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.time.Instant.now
import java.time.temporal.ChronoUnit.MINUTES
import java.util.*

class Handler : RequestHandler<APIGatewayV2HTTPEvent, String> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val credentialsProvider = EnvironmentVariableCredentialsProvider.create()
    private val dynamoDbClient = DynamoDbClient.builder().credentialsProvider(credentialsProvider).build()
    private val secretsManagerClient = SecretsManagerClient.builder().credentialsProvider(credentialsProvider).build()

    private val bitwardenCredentialsProvider = SecretsManagerCredentialsProvider("bitwarden", secretsManagerClient)
    private val bitwardenClient = BitwardenClient(BitwardenSettings()).apply {
        auth().loginAccessToken(bitwardenCredentialsProvider.clientSecret, "build/bitwarden-state")
    }

    private val botTokenProvider = BitwardenApiKeyProvider("telegram", bitwardenCredentialsProvider, bitwardenClient)
    private val botClient = TelegramBotClient(botTokenProvider)

    private val tokenStore = DynamoDbTokenStore(dynamoDbClient)

    private val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenCredentialsProvider, bitwardenClient)
    private val googleAuthClient = GoogleAuthClient()
    private val googleTokenProvider = CachedAccessTokenProvider(googleCredentialsProvider, tokenStore, googleAuthClient)
    private val googleAuthorizationUrl = "http://accounts.google.com/o/oauth2/v2/auth" +
            "?client_id=${googleCredentialsProvider.clientId}" +
            "&redirect_uri=https://atqbfgeqvzph6jtw7jpsoocdou0mwueu.lambda-url.ap-southeast-1.on.aws" +
            "&response_type=code" +
            "&scope=openid%20email%20profile%20https://www.googleapis.com/auth/gmail.readonly" +
            "&access_type=offline" +
            "&prompt=consent"

    private val ynabCredentialsProvider = BitwardenCredentialsProvider("ynab", bitwardenCredentialsProvider, bitwardenClient)
    private val ynabAuthorizationUrl = "https://app.ynab.com/oauth/authorize" +
            "?client_id=${ynabCredentialsProvider.clientId}" +
            "&redirect_uri=https://4oqog5n6uembj6goyhto2gbfeu0lhvep.lambda-url.ap-southeast-1.on.aws" +
            "&response_type=code"

    override fun handleRequest(event: APIGatewayV2HTTPEvent, context: Context) = runBlocking {
        val body = Json.decodeFromString<JsonObject>(event.body!!)
        val message = body["message"]?.jsonObject ?: return@runBlocking "OK"

        val chat = message["chat"]?.jsonObject ?: return@runBlocking "OK"
        val chatId = chat["id"]?.jsonPrimitive?.content ?: return@runBlocking "OK"

        val user = message["from"]?.jsonObject ?: return@runBlocking "OK"
        val text = message["text"]?.jsonPrimitive?.content
        val authority = user["id"]?.jsonPrimitive?.content

        if (text.equals("/start", true)) {
            botClient.sendChatAction(chatId, "typing")

            val login = getItem("login", authority).item()
            val userId = login["userId"]?.s() ?: UUID.randomUUID().toString()

            dynamoDbClient.putItem {
                val item = mapOf("id" to fromS(authority), "userId" to fromS(userId))
                it.tableName("login").item(item)
            }

            dynamoDbClient.putItem {
                val item = mapOf("id" to fromS(userId), "loginId" to fromS(authority))
                it.tableName("user").item(item)
            }
        }

        if (text.equals("/authorizegmail", true)) {
            botClient.sendChatAction(chatId, "typing")
            val login = getItem("login", authority).item()
            val userId = login["userId"]?.s()
            val token = UUID.randomUUID().toString()

            val ttlInstant = now().plus(30, MINUTES)
            val ttl = ttlInstant.epochSecond

            updateItem("token", token, "userId", userId)
            updateItem("token", token, "ttl", ttl)

            val message = "[🔑 Authorize Gmail]($googleAuthorizationUrl&state=$token)"
            botClient.sendMessage(chatId, message) {
                parameter("parse_mode", "MarkdownV2")
            }
        }

        if (text.equals("/authorizeynab", true)) {
            botClient.sendChatAction(chatId, "typing")
            val login = getItem("login", authority).item()
            val userId = login["userId"]?.s()
            val token = UUID.randomUUID().toString()

            val ttlInstant = now().plus(30, MINUTES)
            val ttl = ttlInstant.epochSecond

            updateItem("token", token, "userId", userId)
            updateItem("token", token, "ttl", ttl)

            val message = "[🔑 Authorize YNAB]($ynabAuthorizationUrl&state=$token)"
            botClient.sendMessage(chatId, message) {
                parameter("parse_mode", "MarkdownV2")
            }
        }

        if (text.equals("/watchgmail", true)) {
            botClient.sendChatAction(chatId, "typing")
            val login = getItem("login", authority).item()
            val userId = login["userId"]?.s()
            val user = dynamoDbClient.query {
                it.tableName("login")
                    .keyConditionExpression("#i = :i")
                    .expressionAttributeNames(mapOf("#i" to "id"))
                    .expressionAttributeValues(mapOf(":i" to fromS(userId)))
            }.items()

            val emails = user.mapNotNull {
                it["loginId"]?.s()
            }.filter {
                it.endsWith("@gmail.com")
            }

            val topicName = "projects/lexical-cider-458409-d5/topics/gmail"
            val labelName = "money"

            emails.map { gmail ->
                launch(Dispatchers.IO) {
                    val googleGmailClient = GoogleGmailClient(gmail, googleTokenProvider)

                    val labelId = googleGmailClient.getLabels().body<Label.List>().labels
                        ?.find { it.name == labelName }
                        ?.id ?: throw IllegalStateException()

                    googleGmailClient.postWatch(request = WatchRequest(topicName, labelId))
                }
            }

            botClient.sendMessage(chatId, "🔭 I am now watching your gmails labelled *$labelName*") {
                parameter("parse_mode", "MarkdownV2")
            }
        }

        return@runBlocking "OK"
    }

    fun getItem(table: String, id: String?) = dynamoDbClient.getItem {
        val key = mapOf("id" to fromS(id))
        it.tableName(table).key(key)
    }

    suspend fun updateItem(table: String, id: String?, attributeName: String, attributeValue: String?) =
        coroutineScope {
            val updateExpression = "set #a = :a"
            dynamoDbClient.updateItem {
                val key = mapOf("id" to fromS(id))
                it.tableName(table).key(key)
                    .updateExpression(updateExpression)
                    .expressionAttributeNames(mapOf("#a" to attributeName))
                    .expressionAttributeValues(mapOf(":a" to fromS(attributeValue)))
            }
        }

    suspend fun updateItem(table: String, id: String?, attributeName: String, attributeValue: Long?) = coroutineScope {
        val updateExpression = "set #a = :a"
        dynamoDbClient.updateItem {
            val key = mapOf("id" to fromS(id))
            it.tableName(table).key(key)
                .updateExpression(updateExpression)
                .expressionAttributeNames(mapOf("#a" to attributeName))
                .expressionAttributeValues(mapOf(":a" to fromN("$attributeValue")))
        }
    }

}