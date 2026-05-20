package noodle.security.infrastructure.out

import noodle.database.DynamoDbSortRepository
import noodle.security.core.domain.Token
import noodle.security.core.port.TokenRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import kotlin.time.Clock.System.now

class DynamoDbTokenRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(), TokenRepository, noodle.chat.port.out.TokenRepository {
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

    override suspend fun putToken(token: noodle.chat.domain.StateToken) {
        val ttl = (now() + token.duration).epochSeconds
        put(token.id, "state") {
            put("value", fromS(token.userId))
            put("ttl", fromN(ttl.toString()))
        }
    }
}
