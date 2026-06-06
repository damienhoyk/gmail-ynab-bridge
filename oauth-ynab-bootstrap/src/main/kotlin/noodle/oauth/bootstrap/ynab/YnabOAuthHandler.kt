package noodle.oauth.bootstrap.ynab

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import noodle.bitwarden.infrastructure.api.Bitwarden
import noodle.bitwarden.infrastructure.api.BitwardenSecret
import noodle.bitwarden.infrastructure.api.bitwardenSecret
import noodle.oauth.core.domain.AuthorizeCommand
import noodle.oauth.core.service.AuthorizeService
import noodle.oauth.infrastructure.api.ynab.KtorYnabLoginIdProvider
import noodle.oauth.infrastructure.api.ynab.KtorYnabTokenProvider
import noodle.oauth.infrastructure.persistence.DynamoDbLoginRepository
import noodle.oauth.infrastructure.persistence.DynamoDbTokenRepository
import noodle.oauth.infrastructure.persistence.DynamoDbUserRepository
import noodle.ynab.auth.infrastructure.api.YnabAuthApi
import noodle.ynab.infrastructure.api.YnabApi
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

public class YnabOAuthHandler : RequestHandler<APIGatewayV2HTTPEvent, String> {
    private val initScope = CoroutineScope(Default)
    private val log = LoggerFactory.getLogger(javaClass)

    private val credentialsProviderAsync = initScope.async { DefaultCredentialsProvider.builder().build() }
    private val urlConnectionClient = UrlConnectionHttpClient.builder()

    private val dynamoDbClientAsync: Deferred<DynamoDbClient> =
        initScope.async {
            DynamoDbClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val secretsManagerClientAsync: Deferred<SecretsManagerClient> =
        initScope.async {
            SecretsManagerClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val engineAsync = initScope.async { Java.create() }

    private val ynabTokenProviderAsync =
        initScope.async {
            val httpClient = HttpClient(engineAsync.await())
            val ynabAuthApi = YnabAuthApi(httpClient)
            KtorYnabTokenProvider(ynabAuthApi)
        }
    private val ynabLoginProviderAsync: Deferred<KtorYnabLoginIdProvider> =
        initScope.async {
            KtorYnabLoginIdProvider(YnabApi(HttpClient(engineAsync.await())))
        }

    private val redirectUri: String = System.getenv("REDIRECT_URI")?.trim() ?: throw IllegalStateException()
    private val secretId: String = System.getenv("SECRET_ID")?.trim() ?: throw IllegalStateException()

    private val bitwardenAsync: Deferred<Bitwarden> = initScope.async { Bitwarden(secretsManagerClientAsync.await()) }

    private val secretAsync: Deferred<BitwardenSecret> =
        initScope.async {
            val bitwarden = bitwardenAsync.await()
            bitwarden.getSecret(secretId)?.bitwardenSecret()!!
        }

    private val tokenRepository: Deferred<DynamoDbTokenRepository> = initScope.async { DynamoDbTokenRepository(dynamoDbClientAsync.await()) }
    private val userRepository: Deferred<DynamoDbUserRepository> = initScope.async { DynamoDbUserRepository(dynamoDbClientAsync.await()) }
    private val loginRepository: Deferred<DynamoDbLoginRepository> = initScope.async { DynamoDbLoginRepository(dynamoDbClientAsync.await()) }

    private val service: AuthorizeService =
        AuthorizeService(
            clientId = runBlocking { secretAsync.await().clientId!! },
            clientSecret = runBlocking { secretAsync.await().clientSecret!! },
            redirectUri = redirectUri,
            authClient = { ynabTokenProviderAsync.await() },
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
