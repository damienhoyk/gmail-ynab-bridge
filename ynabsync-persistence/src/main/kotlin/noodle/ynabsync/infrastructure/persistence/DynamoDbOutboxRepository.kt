package noodle.ynabsync.infrastructure.persistence

import noodle.database.DynamoDbSortRepository
import noodle.ynabsync.core.port.OutboxRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import kotlin.time.Clock.System.now
import kotlin.time.Duration

class DynamoDbOutboxRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(), OutboxRepository {
    override val name = "outbox"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey: String = "destination"
    override val sortKey = "source"

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
