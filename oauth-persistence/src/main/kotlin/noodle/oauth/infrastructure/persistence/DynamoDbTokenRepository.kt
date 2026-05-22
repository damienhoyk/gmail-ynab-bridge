package noodle.oauth.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.oauth.core.domain.Token
import noodle.oauth.core.port.TokenRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class DynamoDbTokenRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    TokenRepository {
    override val name = "token"

    override val partitionKey = "id"
    override val sortKey = "type"

    override suspend fun getUserId(state: String): String? {
        val sort = "state"
        val item = get(state, sort).item()
        val value = item["value"]?.s()
        return value
    }

    override suspend fun getAccessToken(id: String): String? = get(id, "access").item()["value"]?.s()

    override suspend fun getRefreshToken(id: String): String? = get(id, "refresh").item()["value"]?.s()

    override suspend fun updateTokenValue(
        id: String,
        type: String,
        value: String,
    ): Token {
        val item = update(id, type) { put("value", fromS(value)) }.attributes()
        val id = item["id"]?.s()!!
        val type = item["type"]?.s()!!
        val value = item["value"]?.s()!!
        return Token(id, type, value)
    }
}
