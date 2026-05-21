package noodle.gmailsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.gmailsync.core.domain.Bridge
import noodle.gmailsync.core.port.BridgeRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class DynamoDbBridgeRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment), BridgeRepository {
    override val name = "bridge"

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
