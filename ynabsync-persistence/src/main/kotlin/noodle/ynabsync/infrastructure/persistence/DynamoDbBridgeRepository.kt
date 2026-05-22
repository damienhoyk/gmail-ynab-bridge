package noodle.ynabsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.ynabsync.core.port.BridgeRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class DynamoDbBridgeRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    BridgeRepository {
    override val name = "bridge"

    override val partitionKey = "source"
    override val sortKey = "destination"

    override suspend fun getAccounts(
        source: String,
        destination: String,
    ): Map<String, String> {
        val item = get(source, destination).item()
        return item["accounts"]?.m()?.mapValues { it.value.s() } ?: emptyMap()
    }
}
