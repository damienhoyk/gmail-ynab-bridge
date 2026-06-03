package noodle.oauth.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.oauth.core.domain.Token
import noodle.oauth.core.port.TokenRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

public class DynamoDbTokenRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    TokenRepository {
    public override val name: String = "token"

    public override val partitionKey: String = "id"
    public override val sortKey: String = "type"

    public override suspend fun getUserId(state: String): String? {
        val sort = "state"
        val item = get(state, sort).item()
        val value = item["value"]?.s()
        return value
    }

    public override suspend fun getAccessToken(id: String): String? = get(id, "access").item()["value"]?.s()

    public override suspend fun getRefreshToken(id: String): String? = get(id, "refresh").item()["value"]?.s()

    public override suspend fun updateTokenValue(
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
