package noodle.oauth.bootstrap.ynab

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
import noodle.bitwarden.infrastructure.api.Bitwarden
import noodle.bitwarden.infrastructure.api.bitwardenSecret
import noodle.oauth.core.domain.AuthorizeCommand
import noodle.oauth.core.service.AuthorizeService
import noodle.oauth.infrastructure.api.OAuth2Client
import noodle.oauth.infrastructure.api.ynab.KtorYnabLoginIdProvider
import noodle.oauth.infrastructure.persistence.DynamoDbLoginRepository
import noodle.oauth.infrastructure.persistence.DynamoDbTokenRepository
import noodle.oauth.infrastructure.persistence.DynamoDbUserRepository
import noodle.oauth2.infrastructure.api.OAuth2TokenApi
import noodle.ynab.auth.infrastructure.api.YnabAuthApi
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

public class YnabOAuthHandler : RequestHandler<APIGatewayV2HTTPEvent, String> {
    private val initScope = CoroutineScope(Default)
    private val log = LoggerFactory.getLogger(javaClass)

    private val credentialsProviderAsync = initScope.async { DefaultCredentialsProvider.create() }
    private val urlConnectionClient = UrlConnectionHttpClient.builder()

    private val dynamoDbClientAsync: kotlinx.coroutines.Deferred<DynamoDbClient> =
        initScope.async {
            DynamoDbClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val secretsManagerClientAsync: kotlinx.coroutines.Deferred<SecretsManagerClient> =
        initScope.async {
            SecretsManagerClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val engineAsync = initScope.async { Java.create() }

    private val ynabAuthClient: kotlinx.coroutines.Deferred<OAuth2Client> =
        initScope.async {
            val httpClient = HttpClient(engineAsync.await())
            val oauth2TokenApi = OAuth2TokenApi(httpClient, YnabAuthApi.TOKEN_ENDPOINT)
            OAuth2Client(oauth2TokenApi)
        }
    private val ynabLoginProviderAsync: kotlinx.coroutines.Deferred<KtorYnabLoginIdProvider> = initScope.async { KtorYnabLoginIdProvider(HttpClient(engineAsync.await())) }

    private val redirectUri: String = System.getenv("REDIRECT_URI")?.trim() ?: throw IllegalStateException()
    private val secretId: String = System.getenv("SECRET_ID")?.trim() ?: throw IllegalStateException()

    private val bitwardenAsync: kotlinx.coroutines.Deferred<Bitwarden> = initScope.async { Bitwarden(secretsManagerClientAsync.await()) }

    private val secretAsync: kotlinx.coroutines.Deferred<noodle.bitwarden.infrastructure.api.BitwardenSecret> =
        initScope.async {
            val bitwarden = bitwardenAsync.await()
            bitwarden.getSecret(secretId)?.bitwardenSecret()!!
        }

    private val tokenRepository: kotlinx.coroutines.Deferred<DynamoDbTokenRepository> = initScope.async { DynamoDbTokenRepository(dynamoDbClientAsync.await()) }
    private val userRepository: kotlinx.coroutines.Deferred<DynamoDbUserRepository> = initScope.async { DynamoDbUserRepository(dynamoDbClientAsync.await()) }
    private val loginRepository: kotlinx.coroutines.Deferred<DynamoDbLoginRepository> = initScope.async { DynamoDbLoginRepository(dynamoDbClientAsync.await()) }

    private val service: AuthorizeService =
        AuthorizeService(
            clientId = runBlocking { secretAsync.await().clientId!! },
            clientSecret = runBlocking { secretAsync.await().clientSecret!! },
            redirectUri = redirectUri,
            authClient = { ynabAuthClient.await() },
            loginIdProvider = { ynabLoginProviderAsync.await() },
            tokenRepository = { tokenRepository.await() },
            userRepository = { userRepository.await() },
            loginRepository = { loginRepository.await() },
        )

    public override fun handleRequest(
        request: APIGatewayV2HTTPEvent,
        context: Context?,
    ): String =
        runBlocking {
            val code = request.queryStringParameters?.get("code")
            val state = request.queryStringParameters?.get("state")

            val command = AuthorizeCommand(code, state)
            val statusCode = service.execute(command)

            buildJsonObject { put("statusCode", statusCode) }.toString()
        }
}
