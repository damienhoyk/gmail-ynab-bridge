package noodle.security.authorization.callback

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.call.*
import kotlinx.coroutines.runBlocking
import noodle.home.security.*
import noodle.lambda.event.ApiGatewayEvent
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

abstract class AuthorizationHandler(
    val client: OAuth2TokenProvider
) : RequestHandler<ApiGatewayEvent, String> {

    val log = LoggerFactory.getLogger(javaClass)
    val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val credentialsProvider = EnvironmentVariableCredentialsProvider.create()
    val dynamoDbClient = DynamoDbClient.builder().credentialsProvider(credentialsProvider).build()
    val secretsManagerClient = SecretsManagerClient.builder().credentialsProvider(credentialsProvider).build()

    val tokenStore = DynamoDbTokenStore(dynamoDbClient)

    val redirectUri = System.getenv("REDIRECT_URI")?.trim() ?: throw IllegalStateException()
    val secretId = System.getenv("SECRET_ID")?.trim() ?: throw IllegalStateException()

    val bitwardenCredentialsProvider = SecretsManagerCredentialsProvider("bitwarden", secretsManagerClient)
    val authorityCredentialsProvider = BitwardenCredentialsProvider(secretId, bitwardenCredentialsProvider)

    override fun handleRequest(request: ApiGatewayEvent, context: Context?): String? = runBlocking {
        log.debug("▶️ Start handling request [{}]", request)

        val code = request.queryStringParameters["code"]
        val state = request.queryStringParameters["state"]

        if (code == null) {
            log.info("💩 code is null")
            return@runBlocking lambdaResponse(400)
        }

        if (state == null) {
            log.info("💩 state is null")
            return@runBlocking lambdaResponse(400)
        }

        authorityCredentialsProvider.load()

        val request = OAuth2TokenRequest(
            code,
            authorityCredentialsProvider.clientId!!,
            authorityCredentialsProvider.clientSecret,
            redirectUri
        )

        val response = client.getToken(request).body<TokenResponse>()
        val authority = getAuthority(response)

        if (authority.isNullOrBlank()) {
            return@runBlocking lambdaResponse(401)
        }

        val token = dynamoDbClient.getItem {
            it.tableName("token").key(mapOf("id" to fromS(state)))
        }.item()

        val userId = token["userId"]?.s()

        if (userId.isNullOrEmpty()) {
            log.error("💩 user id is null")
            return@runBlocking lambdaResponse(500)
        }

        log.info("🪪 Updating user login mapping for [{}] ...", userId)

        dynamoDbClient.putItem {
            val item = mapOf("id" to fromS(authority), "userId" to fromS(userId))
            it.tableName("login").item(item)
        }

        dynamoDbClient.putItem {
            val item = mapOf("id" to fromS(userId), "loginId" to fromS(authority))
            it.tableName("user").item(item)
        }

        log.info("🎫 Storing tokens for authorization [{}] ...", authority)

        tokenStore.storeAccessToken(authority, response.accessToken!!)
        tokenStore.storeRefreshToken(authority, response.refreshToken!!)

        return@runBlocking "🔓 Completed authorization!"
    }

    abstract fun getAuthority(response: TokenResponse): String?

    private fun lambdaResponse(statusCode: Int, message: String? = null) = mapOf(
        "statusCode" to statusCode,
        "body" to message?.let { mapOf("message" to it) }
    )
        .filterValues { it != null }
        .let(mapper::writeValueAsString)

}