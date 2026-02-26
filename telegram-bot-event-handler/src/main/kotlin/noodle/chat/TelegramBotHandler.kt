package noodle.chat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import noodle.client.Google
import noodle.client.Telegram
import noodle.security.GoogleAuthClient
import noodle.email.GmailLabel
import noodle.email.MailboxRepository
import noodle.email.GmailProfile
import noodle.email.GmailWatchRequest
import noodle.security.AuthorizationRepository
import noodle.security.Bitwarden
import noodle.security.LoginRepository
import noodle.security.TokenRepository
import noodle.security.apiKey
import noodle.security.clientId
import noodle.security.clientSecret
import noodle.security.content
import noodle.security.jsonObject
import noodle.user.UserRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class TelegramBotHandler : RequestHandler<APIGatewayV2HTTPEvent, String> {

    private val log = LoggerFactory.getLogger(javaClass)
    private val initScope = CoroutineScope(Dispatchers.Default)
    private val mapper = Json { ignoreUnknownKeys = true }

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

    private val bitwardenAsync = initScope.async { Bitwarden(secretsManagerClientAsync.await()) }

    private val googleSecretAsync = initScope.async { bitwardenAsync.await().getSecret("google")?.jsonObject()!! }
    private val googleAuthClientAsync = initScope.async { GoogleAuthClient() }
    private val googleAsync = initScope.async {
        val secret = googleSecretAsync.await()
        Google( secret.clientId!!, secret.clientSecret!!,
            authorizationRepositoryAsync.await(),
            googleAuthClientAsync.await()
        )
    }

    private val googleAuthorizationUrlAsync = initScope.async {
        val googleClientId = googleSecretAsync.await().clientId!!
        val googleRedirectUri = System.getenv("GOOGLE_REDIRECT_URI")?.trim() ?: throw IllegalStateException()
        "http://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=$googleClientId" +
                "&redirect_uri=$googleRedirectUri" +
                "&response_type=code" +
                "&scope=openid%20email%20profile%20https://www.googleapis.com/auth/gmail.readonly" +
                "&access_type=offline" +
                "&prompt=consent"
    }

    private val telegramSecretAsync = initScope.async { bitwardenAsync.await().getSecret("telegram")?.jsonObject()!! }
    private val telegramAsync = initScope.async(Dispatchers.IO) {
        val secret = telegramSecretAsync.await()
        Telegram(secret.apiKey!!)
    }

    private val ynabSecretAsync = initScope.async { bitwardenAsync.await().getSecret("ynab")?.jsonObject()!! }
    private val ynabAuthorizationUrlAsync = initScope.async {
        val secret = ynabSecretAsync.await()
        val ynabClientId = secret.clientId!!
        val ynabRedirectUri = System.getenv("YNAB_REDIRECT_URI")?.trim() ?: throw IllegalStateException()
        "https://app.ynab.com/oauth/authorize" +
                "?client_id=$ynabClientId" +
                "&redirect_uri=$ynabRedirectUri" +
                "&response_type=code"
    }

    private val authorizationRepositoryAsync = initScope.async { AuthorizationRepository(dynamoDbClientAsync.await()) }
    private val userRepositoryAsync = initScope.async { UserRepository(client = dynamoDbClientAsync.await()) }
    private val tokenRepositoryAsync = initScope.async { TokenRepository(client = dynamoDbClientAsync.await()) }
    private val loginRepositoryAsync = initScope.async { LoginRepository(client = dynamoDbClientAsync.await()) }
    private val mailboxRepositoryAsync = initScope.async { MailboxRepository(client = dynamoDbClientAsync.await()) }

    override fun handleRequest(event: APIGatewayV2HTTPEvent, context: Context) = runBlocking {
        val botClient = telegramAsync.await().botClient()

        val body = mapper.decodeFromString<JsonObject>(event.body!!)
        val message = body["message"]?.jsonObject ?: return@runBlocking "OK"

        val chat = message["chat"]?.jsonObject ?: return@runBlocking "OK"
        val chatId = chat["id"]?.content ?: return@runBlocking "OK"

        val user = message["from"]?.jsonObject ?: return@runBlocking "OK"
        val text = message["text"]?.content
        val authority = user["id"]?.content ?: return@runBlocking "OK"

        if (text.equals("/start", true)) {
            val loginRepository = loginRepositoryAsync.await()
            val userRepository = userRepositoryAsync.await()

            botClient.sendChatAction(chatId, "typing")

            val login = loginRepository.get(authority).item()
            val userId = login["userId"]?.s() ?: UUID.randomUUID().toString()

            loginRepository.put(authority) { put("userId", fromS(userId)) }
            userRepository.put(userId, authority)
        }

        if (text.equals("/authorizegmail", true)) {
            val loginRepository = loginRepositoryAsync.await()
            val tokenRepository = tokenRepositoryAsync.await()
            val googleAuthorizationUrl = googleAuthorizationUrlAsync.await()

            botClient.sendChatAction(chatId, "typing")
            val login = loginRepository.get(authority).item()
            val userId = login["userId"]?.s()
            val token = UUID.randomUUID().toString()

            val ttlInstant = Instant.now().plus(30, ChronoUnit.MINUTES)
            val ttl = ttlInstant.epochSecond

            tokenRepository.put(token) {
                put("userId", fromS(userId))
                put("ttl", fromN(ttl.toString()))
            }

            val message = "[🔑 Authorize Gmail]($googleAuthorizationUrl&state=$token)"
            botClient.sendMessage(chatId, message) {
                parameter("parse_mode", "MarkdownV2")
            }
        }

        if (text.equals("/authorizeynab", true)) {
            val loginRepository = loginRepositoryAsync.await()
            val tokenRepository = tokenRepositoryAsync.await()
            val ynabAuthorizationUrl = ynabAuthorizationUrlAsync.await()

            botClient.sendChatAction(chatId, "typing")
            val login = loginRepository.get(authority).item()
            val userId = login["userId"]?.s()
            val token = UUID.randomUUID().toString()

            val ttlInstant = Instant.now().plus(30, ChronoUnit.MINUTES)
            val ttl = ttlInstant.epochSecond

            tokenRepository.put(token) {
                put("userId", fromS(userId))
                put("ttl", fromN(ttl.toString()))
            }

            val message = "[🔑 Authorize YNAB]($ynabAuthorizationUrl&state=$token)"
            botClient.sendMessage(chatId, message) {
                parameter("parse_mode", "MarkdownV2")
            }
        }

        if (text.equals("/watchgmail", true)) {
            val loginRepository = loginRepositoryAsync.await()
            val userRepository = userRepositoryAsync.await()
            val google = googleAsync.await()

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

            val mailboxRepository = mailboxRepositoryAsync.await()

            val jobs = emails.map { gmail ->
                launch {
                    val googleGmailClient = google.gmailClient(gmail)

                    val labels = googleGmailClient.getLabels().body<GmailLabel.List>().labels
                    val profile = googleGmailClient.getProfile().body<GmailProfile>()
                    val state = profile.historyId.toString()

                    val labelIds = labels
                        ?.filter { it.name.equals(labelName, true) }
                        ?.map { it.id } ?: emptyList()

                    mailboxRepository.update(gmail) { put("state", fromN(state)) }
                    googleGmailClient.postWatch(request = GmailWatchRequest(topicName, labelIds))
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