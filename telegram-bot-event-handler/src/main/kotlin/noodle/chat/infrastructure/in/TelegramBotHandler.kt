package noodle.chat.infrastructure.`in`

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType.Application
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import noodle.chat.domain.RespondChatCommand
import noodle.chat.infrastructure.KtorGmailClientFactory
import noodle.chat.infrastructure.out.KtorTelegramBotClient
import noodle.chat.port.`in`.TelegramBotService
import noodle.email.infrastructure.out.DynamoDbMailboxRepository
import noodle.security.Bitwarden
import noodle.security.apiKey
import noodle.security.clientId
import noodle.security.clientSecret
import noodle.security.content
import noodle.security.infrastructure.out.DynamoDbLoginRepository
import noodle.security.infrastructure.out.DynamoDbTokenRepository
import noodle.security.infrastructure.out.KtorGoogleAuthClient
import noodle.security.jsonObject
import noodle.security.port.`in`.AuthTokenService
import noodle.user.infrastructure.out.DynamoDbUserRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

class TelegramBotHandler : RequestHandler<APIGatewayV2HTTPEvent, String> {
    private val log = LoggerFactory.getLogger(javaClass)
    private val initScope = CoroutineScope(Dispatchers.Default)
    private val mapper = Json { ignoreUnknownKeys = true }

    private val credentialsProviderAsync = initScope.async { DefaultCredentialsProvider.create() }

    private val dynamoDbClientAsync =
        initScope.async {
            DynamoDbClient.builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()
        }

    private val secretsManagerClientAsync =
        initScope.async {
            SecretsManagerClient.builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()
        }

    private val bitwardenAsync = initScope.async { Bitwarden(secretsManagerClientAsync.await()) }

    private val engine = CIO.create()

    private val googleSecretAsync =
        initScope.async { bitwardenAsync.await().getSecret("google")?.jsonObject()!! }
    private val ynabSecretAsync =
        initScope.async { bitwardenAsync.await().getSecret("ynab")?.jsonObject()!! }
    private val telegramSecretAsync =
        initScope.async { bitwardenAsync.await().getSecret("telegram")?.jsonObject()!! }

    private val googleAuthClientAsync = initScope.async { KtorGoogleAuthClient(HttpClient(engine)) }
    private val googleAuthTokenService =
        initScope.async {
            val secret = googleSecretAsync.await()
            AuthTokenService(
                clientId = secret.clientId!!,
                clientSecret = secret.clientSecret!!,
                tokenRepository = tokenRepository.await(),
                authClient = googleAuthClientAsync.await(),
            )
        }

    private val gmailClientFactory =
        initScope.async { KtorGmailClientFactory(googleAuthTokenService.await(), engine) }

    private val googleAuthorizationUrl =
        initScope.async {
            val googleClientId = googleSecretAsync.await().clientId!!
            val googleRedirectUri =
                System.getenv("GOOGLE_REDIRECT_URI")?.trim()
                    ?: throw IllegalStateException()
            "http://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=$googleClientId" +
                "&redirect_uri=$googleRedirectUri" +
                "&response_type=code" +
                "&scope=openid%20email%20profile%20https://www.googleapis.com/auth/gmail.readonly" +
                "&access_type=offline" +
                "&prompt=consent"
        }

    private val ynabAuthorizationUrl =
        initScope.async {
            val secret = ynabSecretAsync.await()
            val ynabClientId = secret.clientId!!
            val ynabRedirectUri =
                System.getenv("YNAB_REDIRECT_URI")?.trim() ?: throw IllegalStateException()
            "https://app.ynab.com/oauth/authorize" +
                "?client_id=$ynabClientId" +
                "&redirect_uri=$ynabRedirectUri" +
                "&response_type=code"
        }

    private val telegramBotClient =
        initScope.async(Dispatchers.IO) {
            val secret = telegramSecretAsync.await()
            KtorTelegramBotClient(HttpClient(engine)) {
                defaultRequest {
                    contentType(Application.Json)
                    url("https://api.telegram.org/bot${secret.apiKey!!}/")
                }
            }
        }

    private val userRepository =
        initScope.async { DynamoDbUserRepository(client = dynamoDbClientAsync.await()) }
    private val tokenRepository =
        initScope.async { DynamoDbTokenRepository(client = dynamoDbClientAsync.await()) }
    private val loginRepository =
        initScope.async { DynamoDbLoginRepository(client = dynamoDbClientAsync.await()) }
    private val mailboxRepository =
        initScope.async { DynamoDbMailboxRepository(client = dynamoDbClientAsync.await()) }

    private val service =
        TelegramBotService(
            botClient = { telegramBotClient.await() },
            googleAuthorizationUrl = { googleAuthorizationUrl.await() },
            ynabAuthorizationUrl = { ynabAuthorizationUrl.await() },
            mailboxRepository = { mailboxRepository.await() },
            loginRepository = { loginRepository.await() },
            tokenRepository = { tokenRepository.await() },
            userRepository = { userRepository.await() },
            gmailClientFactory = { gmailClientFactory.await() },
        )

    override fun handleRequest(
        event: APIGatewayV2HTTPEvent,
        context: Context,
    ) = runBlocking {
        val body = mapper.decodeFromString<JsonObject>(event.body!!)
        val message =
            body["message"]?.jsonObject
                ?: return@runBlocking buildJsonObject { put("statusCode", 400) }.toString()

        val chat =
            message["chat"]?.jsonObject
                ?: return@runBlocking buildJsonObject { put("statusCode", 400) }.toString()
        val chatId =
            chat["id"]?.content
                ?: return@runBlocking buildJsonObject { put("statusCode", 400) }.toString()

        val user =
            message["from"]?.jsonObject
                ?: return@runBlocking buildJsonObject { put("statusCode", 400) }.toString()
        val text = message["text"]?.content
        val userId =
            user["id"]?.content
                ?: return@runBlocking buildJsonObject { put("statusCode", 400) }.toString()
        val authority = "$userId@web.telegram.org"

        val command = RespondChatCommand(authority, text!!, chatId)
        val statusCode = service.execute(command)

        return@runBlocking buildJsonObject { put("statusCode", statusCode) }.toString()
    }
}
