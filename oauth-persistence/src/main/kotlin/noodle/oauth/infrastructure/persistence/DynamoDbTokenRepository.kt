package noodle.oauth.infrastructure.persistence

import noodle.database.DynamoDbSortRepository
import noodle.oauth.core.domain.Token
import noodle.oauth.core.port.TokenRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class DynamoDbTokenRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(), TokenRepository {
    override val name = "token"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "id"
    override val sortKey = "type"

    override suspend fun getToken(
        id: String,
        type: String,
    ): Token {
        val item = get(id, type).item()
        val value = item["value"]?.s()!!
        return Token(id, type, value)
    }

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
