package noodle.gmailsync.infrastructure.persistence

import noodle.database.DynamoDbSortRepository
import noodle.gmailsync.core.domain.Outbox
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import noodle.gmailsync.core.port.OutboxRepository as EmailOutbox
import noodle.ynabsync.core.port.OutboxRepository as FinanceOutbox

class DynamoDbOutboxRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(), EmailOutbox, FinanceOutbox {
    override val name = "outbox"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey: String = "destination"
    override val sortKey = "source"

    override suspend fun putOutbox(outbox: Outbox) {
        put(outbox.destination, outbox.source)
    }

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
