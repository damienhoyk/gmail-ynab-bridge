package noodle.oauth.infrastructure.handler.google

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
import noodle.google.infrastructure.api.KtorGoogleAuthClient
import noodle.oauth.core.domain.AuthorizeCommand
import noodle.oauth.core.service.OAuth2Service
import noodle.oauth.infrastructure.persistence.DynamoDbLoginRepository
import noodle.oauth.infrastructure.persistence.DynamoDbTokenRepository
import noodle.serialization.clientId
import noodle.serialization.clientSecret
import noodle.serialization.jsonObject
import noodle.user.infrastructure.persistence.DynamoDbUserRepository
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
            DynamoDbClient.builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val secretsManagerClientAsync =
        initScope.async {
            SecretsManagerClient.builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val engineAsync = initScope.async { Java.create() }
    val googleAuthClient = initScope.async { KtorGoogleAuthClient(HttpClient(engineAsync.await())) }

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
        OAuth2Service(
            clientId = runBlocking { secretAsync.await().clientId!! },
            clientSecret = runBlocking { secretAsync.await().clientSecret!! },
            redirectUri = redirectUri,
            authClient = { googleAuthClient.await() },
            loginIdProvider = { googleAuthClient.await() },
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
