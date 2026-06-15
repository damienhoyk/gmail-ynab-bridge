package noodle.ynabsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.ynabsync.core.domain.Account
import noodle.ynabsync.core.domain.YnabUrn
import noodle.ynabsync.core.port.AccountRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

public class DynamoDbAccountRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    AccountRepository {
    override val name: String = "ynab-account"

    override val partitionKey: String = "owner"
    override val sortKey: String = "id"

    override suspend fun getAccounts(owner: String): List<Account> =
        query(owner).items().mapNotNull { item ->
            val idStr = item[sortKey]?.s() ?: return@mapNotNull null
            val urn = YnabUrn.parse(idStr) ?: return@mapNotNull null
            val bankAccount = item["bankAccount"]?.s()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            Account(bankAccount, urn)
        }
}
