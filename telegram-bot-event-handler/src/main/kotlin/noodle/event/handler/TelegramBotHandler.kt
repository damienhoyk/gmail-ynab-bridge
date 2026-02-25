package noodle.event.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import noodle.google.auth.GoogleAuthClient
import noodle.google.gmail.GoogleGmailClient
import noodle.google.gmail.Label
import noodle.google.gmail.WatchRequest
import noodle.home.security.*
import noodle.telegram.bot.TelegramBotClient
import noodle.repository.LoginRepository
import noodle.repository.TokenRepository
import noodle.repository.UserRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.time.Instant.now
import java.time.temporal.ChronoUnit.MINUTES
import java.util.*

class TelegramBotHandler : RequestHandler<APIGatewayV2HTTPEvent, String> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val initScope = CoroutineScope(Default)

    private val credentialsProviderAsync = initScope.async { EnvironmentVariableCredentialsProvider.create() }
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
        secretsManagerClient.getSecret("bitwarden").jsonObject()
    }

    private val bitwardenClientAsync = initScope.async {
        val bitwardenSecret = bitwardenSecretAsync.await()
        val bitwardenApiKey = bitwardenSecret.clientSecret!!
        bitwardenClient().apply { auth().authorize(bitwardenApiKey) }
    }

    private val botClientAsync = initScope.async {
        val bitwardenSecret = bitwardenSecretAsync.await()
        val bitwardenOrganizationId = bitwardenSecret.clientId!!
        val bitwardenClient = bitwardenClientAsync.await()
        val botTokenProvider = BitwardenApiKeyProvider("telegram", bitwardenClient, bitwardenOrganizationId)
        TelegramBotClient(botTokenProvider)
    }

    private val tokenStoreAsync = initScope.async { DynamoDbTokenStore(dynamoDbClientAsync.await()) }

    private val googleAuthClient = GoogleAuthClient()
    private val googleTokenProviderAsync = initScope.async {
        val bitwardenSecret = bitwardenSecretAsync.await()
        val bitwardenOrganizationId = bitwardenSecret.clientId!!
        val bitwardenClient = bitwardenClientAsync.await()
        val googleCredentialsProvider = BitwardenCredentialsProvider("google", bitwardenClient, bitwardenOrganizationId)
        val tokenStore = tokenStoreAsync.await()
        CachedAccessTokenProvider(googleCredentialsProvider, tokenStore, googleAuthClient)
    }

    private val googleAuthorizationUrlAsync = initScope.async {
        val bitwardenSecret = bitwardenSecretAsync.await()
        val bitwardenOrganizationId = bitwardenSecret.clientId!!
        val bitwardenClient = bitwardenClientAsync.await()
        val googleClientId = bitwardenClient.secrets().getClientId(bitwardenOrganizationId, "google")
        val googleRedirectUri = System.getenv("GOOGLE_REDIRECT_URI")?.trim() ?: throw IllegalStateException()
        "http://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=$googleClientId" +
                "&redirect_uri=$googleRedirectUri" +
                "&response_type=code" +
                "&scope=openid%20email%20profile%20https://www.googleapis.com/auth/gmail.readonly" +
                "&access_type=offline" +
                "&prompt=consent"
    }

    private val ynabAuthorizationUrlAsync = initScope.async {
        val bitwardenSecret = bitwardenSecretAsync.await()
        val bitwardenOrganizationId = bitwardenSecret.clientId!!
        val bitwardenClient = bitwardenClientAsync.await()
        val ynabClientId = bitwardenClient.secrets().getClientId(bitwardenOrganizationId, "ynab")?.jsonObject()
        val ynabRedirectUri = System.getenv("YNAB_REDIRECT_URI")?.trim() ?: throw IllegalStateException()
        "https://app.ynab.com/oauth/authorize" +
                "?client_id=$ynabClientId" +
                "&redirect_uri=$ynabRedirectUri" +
                "&response_type=code"
    }

    private val userRepositoryAsync = initScope.async { UserRepository(client = dynamoDbClientAsync.await()) }
    private val tokenRepositoryAsync = initScope.async { TokenRepository(client = dynamoDbClientAsync.await()) }
    private val loginRepositoryAsync = initScope.async { LoginRepository(client = dynamoDbClientAsync.await()) }

    override fun handleRequest(event: APIGatewayV2HTTPEvent, context: Context) = runBlocking {
        val body = Json.decodeFromString<JsonObject>(event.body!!)
        val message = body["message"]?.jsonObject ?: return@runBlocking "OK"

        val chat = message["chat"]?.jsonObject ?: return@runBlocking "OK"
        val chatId = chat["id"]?.content ?: return@runBlocking "OK"

        val user = message["from"]?.jsonObject ?: return@runBlocking "OK"
        val text = message["text"]?.content
        val authority = user["id"]?.content

        if (text.equals("/start", true)) {
            val botClient = botClientAsync.await()
            val loginRepository = loginRepositoryAsync.await()
            val userRepository = userRepositoryAsync.await()

            botClient.sendChatAction(chatId, "typing")

            val login = loginRepository.get(authority).item()
            val userId = login["userId"]?.s() ?: UUID.randomUUID().toString()

            loginRepository.put(mapOf("id" to fromS(authority), "userId" to fromS(userId)))
            userRepository.put(mapOf("id" to fromS(userId), "loginId" to fromS(authority)))
        }

        if (text.equals("/authorizegmail", true)) {
            val botClient = botClientAsync.await()
            val loginRepository = loginRepositoryAsync.await()
            val tokenRepository = tokenRepositoryAsync.await()
            val googleAuthorizationUrl = googleAuthorizationUrlAsync.await()

            botClient.sendChatAction(chatId, "typing")
            val login = loginRepository.get(authority).item()
            val userId = login["userId"]?.s()
            val token = UUID.randomUUID().toString()

            val ttlInstant = now().plus(30, MINUTES)
            val ttl = ttlInstant.epochSecond


            tokenRepository.put(mapOf(
                "token" to fromS(token),
                "userId" to fromS(userId),
                "ttl" to fromN(ttl.toString())
            ))

            val message = "[🔑 Authorize Gmail]($googleAuthorizationUrl&state=$token)"
            botClient.sendMessage(chatId, message) {
                parameter("parse_mode", "MarkdownV2")
            }
        }

        if (text.equals("/authorizeynab", true)) {
            val botClient = botClientAsync.await()
            val loginRepository = loginRepositoryAsync.await()
            val tokenRepository = tokenRepositoryAsync.await()
            val ynabAuthorizationUrl = ynabAuthorizationUrlAsync.await()

            botClient.sendChatAction(chatId, "typing")
            val login = loginRepository.get(authority).item()
            val userId = login["userId"]?.s()
            val token = UUID.randomUUID().toString()

            val ttlInstant = now().plus(30, MINUTES)
            val ttl = ttlInstant.epochSecond

            tokenRepository.put(mapOf(
                "token" to fromS(token),
                "userId" to fromS(userId),
                "ttl" to fromN(ttl.toString())
            ))

            val message = "[🔑 Authorize YNAB]($ynabAuthorizationUrl&state=$token)"
            botClient.sendMessage(chatId, message) {
                parameter("parse_mode", "MarkdownV2")
            }
        }

        if (text.equals("/watchgmail", true)) {
            val botClient = botClientAsync.await()
            val loginRepository = loginRepositoryAsync.await()
            val userRepository = userRepositoryAsync.await()
            val googleTokenProvider = googleTokenProviderAsync.await()

            botClient.sendChatAction(chatId, "typing")
            val login = loginRepository.get(authority).item()
            val userId = login["userId"]?.s()!!
            val user = userRepository.query(userId).items()

            val emails = user.mapNotNull {
                it["loginId"]?.s()
            }.filter {
                it.endsWith("@gmail.com")
            }

            val topicName = "projects/lexical-cider-458409-d5/topics/gmail"
            val labelName = "money"

            val jobs = emails.map { gmail ->
                launch(Dispatchers.IO) {
                    val googleGmailClient = GoogleGmailClient(gmail, googleTokenProvider)

                    val labelId = googleGmailClient.getLabels().body<Label.List>().labels
                        ?.filter { it.name.equals(labelName, true) }
                        ?.map { it.id } ?: emptyList()

                    googleGmailClient.postWatch(request = WatchRequest(topicName, labelId))
                }
            }

            jobs.joinAll()

            botClient.sendMessage(chatId, "🔭 I am now watching your gmails labelled *$labelName*") {
                parameter("parse_mode", "MarkdownV2")
            }
        }

        return@runBlocking "OK"
    }

}