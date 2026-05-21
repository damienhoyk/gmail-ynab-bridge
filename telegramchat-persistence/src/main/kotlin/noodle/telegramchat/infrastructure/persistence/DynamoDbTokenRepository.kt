package noodle.telegramchat.infrastructure.persistence

import noodle.database.DynamoDbSortRepository
import noodle.telegramchat.core.domain.StateToken
import noodle.telegramchat.core.port.TokenRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import kotlin.time.Clock.System.now

class DynamoDbTokenRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(), TokenRepository {
    override val name = "token"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "id"
    override val sortKey = "type"

    override suspend fun putToken(token: StateToken) {
        val ttl = (now() + token.duration).epochSeconds
        put(token.id, "state") {
            put("value", fromS(token.userId))
            put("ttl", fromN(ttl.toString()))
        }
    }
}
