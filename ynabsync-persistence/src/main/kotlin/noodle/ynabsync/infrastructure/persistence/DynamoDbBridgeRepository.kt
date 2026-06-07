package noodle.ynabsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.ynabsync.core.port.BridgeRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

public class DynamoDbBridgeRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    BridgeRepository {
    override val name: String = "bridge"

    override val partitionKey: String = "source"
    override val sortKey: String = "destination"

    override suspend fun getAccounts(
        mailAddress: String,
        destination: String,
    ): Map<String, String> {
        val item = get(mailAddress, destination).item()
        return item["accounts"]?.m()?.mapValues { it.value.s() } ?: emptyMap()
    }
}
