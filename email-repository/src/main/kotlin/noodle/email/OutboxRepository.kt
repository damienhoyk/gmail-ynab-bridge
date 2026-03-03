package noodle.email

import noodle.database.DynamoDbSortRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class OutboxRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null
) : DynamoDbSortRepository() {

    override val name = "outbox"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey: String = "destination"
    override val sortKey = "source"

}