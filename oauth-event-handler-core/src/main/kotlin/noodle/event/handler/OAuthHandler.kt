package noodle.event.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.ktor.client.call.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import noodle.home.security.*
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

abstract class OAuthHandler(
    val client: OAuth2TokenProvider
) : RequestHandler<APIGatewayV2HTTPEvent, String> {

    val log = LoggerFactory.getLogger(javaClass)

    val dynamoDbClient = DynamoDbClient.builder().build()
    val secretsManagerClient = SecretsManagerClient.builder().build()

    val tokenStore = DynamoDbTokenStore(dynamoDbClient)

    val redirectUri = System.getenv("REDIRECT_URI")?.trim() ?: throw IllegalStateException()
    val secretId = System.getenv("SECRET_ID")?.trim() ?: throw IllegalStateException()

    val bitwardenSecret = runBlocking { secretsManagerClient.getSecret("bitwarden").jsonObject() }
    val bitwardenApiKey = bitwardenSecret.apiKey!!
    val bitwardenClientId = bitwardenSecret.clientId!!

    val bitwardenClient = runBlocking { bitwardenClient().apply { auth().authorize(bitwardenApiKey) } }

    override fun handleRequest(request: APIGatewayV2HTTPEvent, context: Context?): String? = runBlocking {
        log.debug("▶️ Start handling request [{}]", request)

        val code = request.queryStringParameters?.get("code")
        val state = request.queryStringParameters?.get("state")

        if (code == null) {
            log.info("💩 code is null")
            return@runBlocking lambdaResponse(400)
        }

        if (state == null) {
            log.info("💩 state is null")
            return@runBlocking lambdaResponse(400)
        }

        val secret = bitwardenClient.secrets().getSecret(bitwardenClientId, secretId)?.jsonObject()

        val clientId = secret?.clientId
        val clientSecret = secret?.clientSecret

        val request = OAuth2TokenRequest(
            code,
            clientId,
            clientSecret,
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

    private fun lambdaResponse(statusCode: Int, message: String? = null) = buildJsonObject {
        put("statusCode", statusCode)
        putJsonObject("body") {
            put("message", message)
        }
    }.toString()

}