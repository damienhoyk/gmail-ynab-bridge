package noodle.bridge.infrastructure.out

import noodle.database.DynamoDbSortRepository
import noodle.email.domain.Bridge
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import noodle.email.port.out.BridgeRepository as EmailBridgeRepository
import noodle.finance.port.out.BridgeRepository as FinanceBridgeRepository

class DynamoDbBridgeRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(), EmailBridgeRepository, FinanceBridgeRepository {
    override val name = "bridge"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "source"
    override val sortKey = "destination"

    override suspend fun queryBridges(source: String) =
        super.query(source).items()
            .map {
                val source = it["source"]?.s()!!
                val destination = it["destination"]?.s()!!
                Bridge(source, destination)
            }

    override suspend fun getBridge(
        source: String,
        destination: String,
    ): noodle.finance.domain.Bridge {
        val item = get(source, destination).item()
        val source = item["source"]?.s()!!
        val destination = item["destination"]?.s()!!
        val accounts = item["accounts"]?.m()?.mapValues { it.value.s() } ?: emptyMap()
        return noodle.finance.domain.Bridge(source, destination, accounts)
    }
}
