package noodle.oauth.bootstrap.google

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
import noodle.google.auth.infrastructure.api.GoogleOAuth2Api
import noodle.oauth.core.domain.AuthorizeCommand
import noodle.oauth.core.service.AuthorizeService
import noodle.oauth.infrastructure.api.google.KtorGoogleLoginIdProvider
import noodle.oauth.infrastructure.api.google.KtorGoogleTokenProvider
import noodle.oauth.infrastructure.persistence.DynamoDbLoginRepository
import noodle.oauth.infrastructure.persistence.DynamoDbTokenRepository
import noodle.oauth.infrastructure.persistence.DynamoDbUserRepository
import noodle.oauth2.infrastructure.api.OidcApi
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

public class GoogleOAuthHandler : RequestHandler<APIGatewayV2HTTPEvent, String> {
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
    private val googleTokenProviderAsync =
        initScope.async {
            val httpClient = HttpClient(engineAsync.await())
            val oidcApi = OidcApi("https://accounts.google.com/.well-known/openid-configuration", httpClient)
            KtorGoogleTokenProvider(oidcApi)
        }
    private val googleLoginProviderAsync: Deferred<KtorGoogleLoginIdProvider> = initScope.async { KtorGoogleLoginIdProvider(GoogleOAuth2Api(HttpClient(engineAsync.await()))) }

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
            authClient = { googleTokenProviderAsync.await() },
            loginIdProvider = { googleLoginProviderAsync.await() },
            tokenRepository = { tokenRepository.await() },
            userRepository = { userRepository.await() },
            loginRepository = { loginRepository.await() },
            refreshTokenTtlSeconds = System.getenv("REFRESH_TOKEN_TTL_SECONDS")?.trim()?.toLong() ?: 15_552_000,
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
