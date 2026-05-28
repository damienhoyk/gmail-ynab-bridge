package noodle.oauth.bootstrap.google

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import noodle.bitwarden.Bitwarden
import noodle.google.auth.infrastructure.api.GoogleOAuth2Api
import noodle.oauth.core.domain.AuthorizeCommand
import noodle.oauth.core.service.AuthorizeService
import noodle.oauth.infrastructure.api.OidcApi
import noodle.oauth.infrastructure.api.google.KtorGoogleOAuth2Client
import noodle.oauth.infrastructure.api.google.KtorGoogleOidcClient
import noodle.oauth.infrastructure.persistence.DynamoDbLoginRepository
import noodle.oauth.infrastructure.persistence.DynamoDbTokenRepository
import noodle.oauth.infrastructure.persistence.DynamoDbUserRepository
import noodle.serialization.clientId
import noodle.serialization.clientSecret
import noodle.serialization.jsonObject
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

class GoogleOAuthHandler : RequestHandler<APIGatewayV2HTTPEvent, String> {
    private val initScope = CoroutineScope(Default)
    private val log = LoggerFactory.getLogger(javaClass)

    private val credentialsProviderAsync = initScope.async { DefaultCredentialsProvider.create() }
    private val urlConnectionClient = UrlConnectionHttpClient.builder()

    private val dynamoDbClientAsync =
        initScope.async {
            DynamoDbClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val secretsManagerClientAsync =
        initScope.async {
            SecretsManagerClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val engineAsync = initScope.async { Java.create() }
    val googleOidcClient = initScope.async { KtorGoogleOidcClient(OidcApi(HttpClient(engineAsync.await()), "https://accounts.google.com/.well-known/openid-configuration")) }
    val googleLoginProviderAsync = initScope.async { KtorGoogleOAuth2Client(GoogleOAuth2Api(HttpClient(engineAsync.await()))) }

    val redirectUri = System.getenv("REDIRECT_URI")?.trim() ?: throw IllegalStateException()
    val secretId = System.getenv("SECRET_ID")?.trim() ?: throw IllegalStateException()

    val bitwardenAsync = initScope.async { Bitwarden(secretsManagerClientAsync.await()) }

    val secretAsync =
        initScope.async {
            val bitwarden = bitwardenAsync.await()
            bitwarden.getSecret(secretId)?.jsonObject()!!
        }

    val tokenRepository = initScope.async { DynamoDbTokenRepository(dynamoDbClientAsync.await()) }
    val userRepository = initScope.async { DynamoDbUserRepository(dynamoDbClientAsync.await()) }
    val loginRepository = initScope.async { DynamoDbLoginRepository(dynamoDbClientAsync.await()) }

    val service =
        AuthorizeService(
            clientId = runBlocking { secretAsync.await().clientId!! },
            clientSecret = runBlocking { secretAsync.await().clientSecret!! },
            redirectUri = redirectUri,
            authClient = { googleOidcClient.await() },
            loginIdProvider = { googleLoginProviderAsync.await() },
            tokenRepository = { tokenRepository.await() },
            userRepository = { userRepository.await() },
            loginRepository = { loginRepository.await() },
        )

    override fun handleRequest(
        request: APIGatewayV2HTTPEvent,
        context: Context?,
    ) = runBlocking {
        val code = request.queryStringParameters?.get("code")
        val state = request.queryStringParameters?.get("state")

        val command = AuthorizeCommand(code, state)
        val statusCode = service.execute(command)

        buildJsonObject { put("statusCode", statusCode) }.toString()
    }
}
