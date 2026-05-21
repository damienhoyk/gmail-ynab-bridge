package noodle.gmailsync.infrastructure.persistence

import noodle.database.DynamoDbSortRepository
import noodle.gmailsync.core.domain.Bridge
import noodle.gmailsync.core.port.BridgeRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class DynamoDbBridgeRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(), BridgeRepository {
    override val name = "bridge"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "source"
    override val sortKey = "destination"

    override suspend fun queryBridge(source: String) =
        query(source).items()
            .map {
                val source = it["source"]?.s()!!
                val destination = it["destination"]?.s()!!
                Bridge(source, destination)
            }
}
