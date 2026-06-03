package noodle.ynabsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.ynabsync.core.port.OutboxRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import kotlin.time.Clock.System.now
import kotlin.time.Duration

public class DynamoDbOutboxRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    OutboxRepository {
    override val name: String = "outbox"

    override val partitionKey: String = "destination"
    override val sortKey: String = "source"

    override suspend fun updateTtl(
        destination: String,
        source: String,
        duration: Duration,
    ): Long {
        val item =
            update(destination, source) {
                put("ttl", fromN("${(now() + duration).epochSeconds}"))
            }.attributes()
        return item["ttl"]?.n()?.toLong()!!
    }
}
