package noodle.telegramchat.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.telegramchat.core.domain.StateToken
import noodle.telegramchat.core.port.TokenRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import kotlin.time.Clock.System.now

public class DynamoDbTokenRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    TokenRepository {
    override val name: String = "token"

    override val partitionKey: String = "id"
    override val sortKey: String = "type"

    override suspend fun putToken(token: StateToken) {
        val ttl = (now() + token.duration).epochSeconds
        put(token.id, "state") {
            put("value", fromS(token.userId))
            put("ttl", fromN(ttl.toString()))
        }
    }
}
