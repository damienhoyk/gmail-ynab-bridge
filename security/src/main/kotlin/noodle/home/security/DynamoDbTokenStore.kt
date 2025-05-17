package noodle.home.security

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class DynamoDbTokenStore(
    val dynamoDbClient: DynamoDbClient = DynamoDbClient.create(),
    val tableName: String = "authorization",
    prefix: String? = null,
    accessTokenAttributeName: String = "accessToken",
    refreshTokenAttributeName: String = "refreshToken"
) : TokenStore {

    val accessTokenAttributeName = prefix?.let { prefix ->
        val suffix = accessTokenAttributeName.replaceFirstChar { it.uppercase() }
        "$prefix$suffix"
    } ?: accessTokenAttributeName

    val refreshTokenAttributeName = prefix?.let { prefix ->
        val suffix = refreshTokenAttributeName.replaceFirstChar { it.uppercase() }
        "$prefix$suffix"
    } ?: refreshTokenAttributeName

    fun getItem(id: String): Map<String?, AttributeValue?> {
        return dynamoDbClient.getItem {
            val key = mapOf("id" to fromS(id))
            it.tableName(tableName).key(key)
        }.item()
    }

    override fun getAccessToken(id: String): String? {
        val item = getItem(id)
        return item[accessTokenAttributeName]?.s()
    }

    override fun getRefreshToken(id: String): String? {
        val item = getItem(id)
        return item[refreshTokenAttributeName]?.s()
    }

    override fun storeAccessToken(id: String, accessToken: String) {
        storeToken(id, accessTokenAttributeName, accessToken)
    }

    override fun storeRefreshToken(id: String, refreshToken: String) {
        storeToken(id, refreshTokenAttributeName, refreshToken)
    }

    private fun storeToken(id: String, attribute: String, token: String) {
        dynamoDbClient.updateItem {
            val key = mapOf("id" to fromS(id))
            val names = mapOf("#t" to attribute)
            val values = mapOf(":t" to fromS(token))

            it.tableName(tableName).key(key)
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .updateExpression("set #t = :t")
        }
    }

}