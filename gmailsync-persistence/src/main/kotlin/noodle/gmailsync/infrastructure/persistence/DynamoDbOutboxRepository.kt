package noodle.gmailsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.gmailsync.core.domain.Outbox
import noodle.gmailsync.core.port.OutboxRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

public class DynamoDbOutboxRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    OutboxRepository {
    override val name: String = "outbox"

    override val partitionKey: String = "destination"
    override val sortKey: String = "source"

    override suspend fun putOutbox(outbox: Outbox) {
        put(outbox.destination, outbox.source)
    }
}
