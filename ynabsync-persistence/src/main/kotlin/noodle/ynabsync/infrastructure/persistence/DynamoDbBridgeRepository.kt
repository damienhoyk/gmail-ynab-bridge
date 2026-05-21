package noodle.ynabsync.infrastructure.persistence

import noodle.database.DynamoDbSortRepository
import noodle.ynabsync.core.domain.Bridge
import noodle.ynabsync.core.port.BridgeRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class DynamoDbBridgeRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(), BridgeRepository {
    override val name = "bridge"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "source"
    override val sortKey = "destination"

    override suspend fun getBridge(
        source: String,
        destination: String,
    ): Bridge {
        val item = get(source, destination).item()
        val source = item["source"]?.s()!!
        val destination = item["destination"]?.s()!!
        val accounts = item["accounts"]?.m()?.mapValues { it.value.s() } ?: emptyMap()
        return Bridge(source, destination, accounts)
    }
}
