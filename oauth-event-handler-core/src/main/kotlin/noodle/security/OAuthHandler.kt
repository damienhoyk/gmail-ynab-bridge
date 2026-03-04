package noodle.security

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.ktor.client.call.*
import io.ktor.http.headers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import noodle.user.UserRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

abstract class OAuthHandler(
    val client: OAuth2TokenProvider
) : RequestHandler<APIGatewayV2HTTPEvent, String> {

    val log = LoggerFactory.getLogger(javaClass)
    val initScope = CoroutineScope(Default)

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

    val redirectUri = System.getenv("REDIRECT_URI")?.trim() ?: throw IllegalStateException()
    val secretId = System.getenv("SECRET_ID")?.trim() ?: throw IllegalStateException()

    val bitwardenAsync = initScope.async { Bitwarden(secretsManagerClientAsync.await()) }

    val loginRepositoryAsync = initScope.async { LoginRepository(client = dynamoDbClientAsync.await()) }
    val userRepositoryAsync = initScope.async { UserRepository(client = dynamoDbClientAsync.await()) }
    val tokenRepositoryAsync = initScope.async { TokenRepository(client = dynamoDbClientAsync.await()) }

    override fun handleRequest(request: APIGatewayV2HTTPEvent, context: Context?): String? = runBlocking {
        log.debug("▶️ Start handling request [{}]", request)

        val code = request.queryStringParameters?.get("code")
        val state = request.queryStringParameters?.get("state")

        if (code == null) {
            log.error("💩 code is null")
            return@runBlocking buildJsonObject {
                put("statusCode", 400)
                put("body", "⛔ Failed authorization")
            }.toString()
        }

        if (state == null) {
            log.error("💩 state is null")
            return@runBlocking buildJsonObject {
                put("statusCode", 400)
                put("body", "⛔ Failed authorization")
            }.toString()
        }

        val bitwarden = bitwardenAsync.await()
        val secret = bitwarden.getSecret(secretId)?.jsonObject()!!

        val clientId = secret.clientId
        val clientSecret = secret.clientSecret

        val request = OAuth2TokenRequest(
            code,
            clientId,
            clientSecret,
            redirectUri
        )

        val response = client.getToken(request).body<TokenResponse>()
        val authority = getAuthority(response)

        if (authority.isNullOrBlank()) {
            log.error("🐳 authority is null")
            return@runBlocking buildJsonObject {
                put("statusCode", 500)
                put("body", "🐳 Internal server error")
            }.toString()
        }

        val tokenRepository = tokenRepositoryAsync.await()
        val token = tokenRepository.get(state, "state").item()

        val userId = token["value"]?.s()

        if (userId.isNullOrEmpty()) {
            log.error("🐳 user id is null")
            return@runBlocking buildJsonObject {
                put("statusCode", 500)
                put("body", "🐳 Internal server error")
            }.toString()
        }

        log.info("🪪 Updating user login mapping for [{}] ...", userId)

        val loginRepository = loginRepositoryAsync.await()
        loginRepository.put(authority) { put("userId", fromS(userId)) }

        val userRepository = userRepositoryAsync.await()
        userRepository.put(userId, authority)

        log.info("🎫 Storing tokens for authorization [{}] ...", authority)

        launch {
            tokenRepository.update(authority, "access") {
                put("value", fromS(response.accessToken))
            }
        }

        launch {
            tokenRepository.update(authority, "refresh") {
                put("value", fromS(response.refreshToken))
            }
        }

        return@runBlocking buildJsonObject {
            put("statusCode", 200)
            put("body", "🔓 Completed authorization!")
        }.toString()
    }

    abstract fun getAuthority(response: TokenResponse): String?

}