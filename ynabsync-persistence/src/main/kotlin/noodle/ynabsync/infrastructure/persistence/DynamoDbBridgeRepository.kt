package noodle.ynabsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.ynabsync.core.domain.Bridge
import noodle.ynabsync.core.domain.YnabUrn
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

    override suspend fun getBridges(mailAddress: String): List<Bridge> =
        query(mailAddress).items().mapNotNull { item ->
            val destinationStr = item[sortKey]?.s() ?: return@mapNotNull null
            val urn = YnabUrn.parse(destinationStr) ?: return@mapNotNull null
            val bankAccount = item["bankAccount"]?.s()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            Bridge(bankAccount, urn)
        }
}
