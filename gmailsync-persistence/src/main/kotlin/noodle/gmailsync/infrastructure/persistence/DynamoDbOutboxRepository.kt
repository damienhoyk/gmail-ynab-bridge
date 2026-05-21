package noodle.gmailsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.gmailsync.core.domain.Outbox
import noodle.gmailsync.core.port.OutboxRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class DynamoDbOutboxRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment), OutboxRepository {
    override val name = "outbox"

    override val partitionKey: String = "destination"
    override val sortKey = "source"

    override suspend fun putOutbox(outbox: Outbox) {
        put(outbox.destination, outbox.source)
    }
}
