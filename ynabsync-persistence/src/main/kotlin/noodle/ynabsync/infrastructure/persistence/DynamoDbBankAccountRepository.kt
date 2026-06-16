package noodle.ynabsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.ynabsync.core.domain.BankAccount
import noodle.ynabsync.core.port.BankAccountRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

/**
 * DynamoDB adapter for the `bank-account` table, which maps a bank account (email, number)
 * to its YNAB account targets.
 */
public class DynamoDbBankAccountRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    BankAccountRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    override val name: String = "bank-account"

    override val partitionKey: String = "partition"
    override val sortKey: String = "sort"

    private companion object {
        val ACCOUNT_PATH = Regex("^/budget/([^/]+)/account/([^/]+)$")
    }

    /**
     * Retrieves bank accounts for the given email and account number.
     *
     * Builds the partition URI `noodle.ynabsync://<email>/account/<number>`, queries the
     * DynamoDB table, and parses each `sort` value into a [BankAccount]. Sort keys that
     * are absent, malformed, or fail validation (scheme `noodle.ynabsync`, host `app.ynab.com`
     * — both case-insensitive — non-blank userInfo, and a `/budget/<id>/account/<id>` path)
     * are logged and skipped.
     */
    override suspend fun getAccounts(
        email: String,
        number: String,
    ): List<BankAccount> {
        val partition = "noodle.ynabsync://$email/account/$number"

        val parsed =
            query(partition).items().map { item ->
                val sort = item[sortKey]?.s()
                sort to
                    sort?.let { runCatching { URI(it) }.getOrNull() }
            }

        val (valid, invalid) =
            parsed.partition { (_, uri) ->
                uri != null &&
                    uri.scheme.equals("noodle.ynabsync", true) &&
                    uri.host.equals("app.ynab.com", true) &&
                    !uri.userInfo.isNullOrBlank() &&
                    ACCOUNT_PATH.matches(uri.path)
            }
        invalid.forEach { (sort, _) -> log.warn("Skipping malformed bank-account sort key [{}]", sort) }

        return valid.map { (_, uri) ->
            val (budgetId, accountId) = ACCOUNT_PATH.matchEntire(uri!!.path)!!.destructured
            BankAccount(email, number, uri.userInfo, budgetId, accountId)
        }
    }
}
