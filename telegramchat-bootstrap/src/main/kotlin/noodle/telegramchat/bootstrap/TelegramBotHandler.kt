package noodle.telegramchat.bootstrap

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType.Application
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import noodle.bitwarden.Bitwarden
import noodle.oauth.core.service.TokenService
import noodle.oauth.infrastructure.api.google.KtorGoogleAuthClientAdapter
import noodle.serialization.apiKey
import noodle.serialization.clientId
import noodle.serialization.clientSecret
import noodle.serialization.jsonObject
import noodle.telegram.infrastructure.api.model.TelegramWebhookEvent
import noodle.telegramchat.core.domain.RespondChatCommand
import noodle.telegramchat.core.service.TelegramBotService
import noodle.telegramchat.infrastructure.api.KtorGmailClientFactory
import noodle.telegramchat.infrastructure.api.KtorTelegramBotClientAdapter
import noodle.telegramchat.infrastructure.persistence.DynamoDbLoginRepository
import noodle.telegramchat.infrastructure.persistence.DynamoDbMailboxRepository
import noodle.telegramchat.infrastructure.persistence.DynamoDbUserRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import noodle.oauth.infrastructure.persistence.DynamoDbTokenRepository as SecurityTokenRepository
import noodle.telegramchat.infrastructure.persistence.DynamoDbTokenRepository as TelegramTokenRepository

class TelegramBotHandler : RequestHandler<APIGatewayV2HTTPEvent, String> {
    private val log = LoggerFactory.getLogger(javaClass)
    private val initScope = CoroutineScope(Dispatchers.Default)
    private val mapper = Json { ignoreUnknownKeys = true }

    private val credentialsProviderAsync = initScope.async { DefaultCredentialsProvider.create() }

    private val dynamoDbClientAsync =
        initScope.async {
            DynamoDbClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()
        }

    private val secretsManagerClientAsync =
        initScope.async {
            SecretsManagerClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()
        }

    private val bitwardenAsync = initScope.async { Bitwarden(secretsManagerClientAsync.await()) }

    private val engineAsync = initScope.async { Java.create() }

    private val googleSecretAsync =
        initScope.async { bitwardenAsync.await().getSecret("google")?.jsonObject()!! }
    private val ynabSecretAsync =
        initScope.async { bitwardenAsync.await().getSecret("ynab")?.jsonObject()!! }
    private val telegramSecretAsync =
        initScope.async { bitwardenAsync.await().getSecret("telegram")?.jsonObject()!! }

    private val googleAuthClientAsync = initScope.async { KtorGoogleAuthClientAdapter(HttpClient(engineAsync.await())) }
    private val googleAuthTokenService =
        initScope.async {
            val secret = googleSecretAsync.await()
            TokenService(
                clientId = secret.clientId!!,
                clientSecret = secret.clientSecret!!,
                tokenRepository = securityTokenRepository.await(),
                authClient = googleAuthClientAsync.await(),
            )
        }

    private val gmailClientFactory =
        initScope.async { KtorGmailClientFactory(googleAuthTokenService.await(), engineAsync.await()) }

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
            KtorTelegramBotClientAdapter(HttpClient(engineAsync.await())) {
                defaultRequest {
                    contentType(Application.Json)
                    url("https://api.telegram.org/bot${secret.apiKey!!}/")
                }
            }
        }

    private val securityTokenRepository =
        initScope.async { SecurityTokenRepository(client = dynamoDbClientAsync.await()) }
    private val telegramUserRepository =
        initScope.async { DynamoDbUserRepository(client = dynamoDbClientAsync.await()) }
    private val telegramTokenRepository =
        initScope.async { TelegramTokenRepository(client = dynamoDbClientAsync.await()) }
    private val telegramLoginRepository =
        initScope.async { DynamoDbLoginRepository(client = dynamoDbClientAsync.await()) }
    private val telegramMailboxRepository =
        initScope.async { DynamoDbMailboxRepository(client = dynamoDbClientAsync.await()) }

    private val service =
        TelegramBotService(
            botClient = { telegramBotClient.await() },
            googleAuthorizationUrl = { googleAuthorizationUrl.await() },
            ynabAuthorizationUrl = { ynabAuthorizationUrl.await() },
            topicName =
                System.getenv("GMAIL_PUBSUB_TOPIC_NAME")?.trim()
                    ?: "projects/lexical-cider-458409-d5/topics/gmail",
            labelName = System.getenv("GMAIL_WATCH_LABEL_NAME")?.trim() ?: "money",
            mailboxRepository = { telegramMailboxRepository.await() },
            loginRepository = { telegramLoginRepository.await() },
            tokenRepository = { telegramTokenRepository.await() },
            userRepository = { telegramUserRepository.await() },
            gmailClientFactory = { gmailClientFactory.await() },
        )

    override fun handleRequest(
        event: APIGatewayV2HTTPEvent,
        context: Context,
    ) = runBlocking {
        val webhookEvent = mapper.decodeFromString<TelegramWebhookEvent>(event.body!!)
        val command =
            RespondChatCommand(
                telegramUserId =
                    webhookEvent.message
                        ?.from
                        ?.id
                        ?.toString(),
                text = webhookEvent.message?.text,
                chatId =
                    webhookEvent.message
                        ?.chat
                        ?.id
                        ?.toString(),
            )

        val statusCode = service.execute(command)

        return@runBlocking buildJsonObject { put("statusCode", statusCode) }.toString()
    }
}
