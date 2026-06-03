package noodle.gmailsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.gmailsync.core.domain.Bridge
import noodle.gmailsync.core.port.BridgeRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

public class DynamoDbBridgeRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    BridgeRepository {
    override val name: String = "bridge"

    override val partitionKey: String = "source"
    override val sortKey: String = "destination"

    override suspend fun queryBridge(source: String): List<Bridge> =
        query(source)
            .items()
            .map {
                val source = it["source"]?.s()!!
                val destination = it["destination"]?.s()!!
                Bridge(source, destination)
            }
}
