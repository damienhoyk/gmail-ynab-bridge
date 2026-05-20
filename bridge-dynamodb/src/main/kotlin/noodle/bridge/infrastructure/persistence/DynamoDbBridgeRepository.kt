package noodle.bridge.infrastructure.persistence

import noodle.database.DynamoDbSortRepository
import noodle.email.core.domain.Bridge
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import noodle.email.core.port.BridgeRepository as EmailBridgeRepository
import noodle.finance.core.port.BridgeRepository as FinanceBridgeRepository

class DynamoDbBridgeRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(), EmailBridgeRepository, FinanceBridgeRepository {
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

    override suspend fun getBridge(
        source: String,
        destination: String,
    ): noodle.finance.core.domain.Bridge {
        val item = get(source, destination).item()
        val source = item["source"]?.s()!!
        val destination = item["destination"]?.s()!!
        val accounts = item["accounts"]?.m()?.mapValues { it.value.s() } ?: emptyMap()
        return noodle.finance.core.domain.Bridge(source, destination, accounts)
    }
}
