package noodle.ynabsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.uri.namedSegment
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

        val (valid, invalid) =
            query(partition).items().partition { item ->
                val sort = item[sortKey]?.s()
                val uri = sort?.let { runCatching { URI(it) }.getOrNull() }
                uri != null &&
                    uri.scheme.equals("noodle.ynabsync", true) &&
                    uri.host.equals("app.ynab.com", true) &&
                    !uri.userInfo.isNullOrBlank() &&
                    !uri.namedSegment("budget").isNullOrBlank() &&
                    !uri.namedSegment("account").isNullOrBlank()
            }
        invalid.forEach { item -> log.warn("Skipping malformed bank-account sort key [{}]", item[sortKey]) }

        return valid.map { item ->
            val uri = item[sortKey]?.s()?.let { URI(it) }
            BankAccount(email, number, uri!!.userInfo, uri.namedSegment("budget")!!, uri.namedSegment("account")!!)
        }
    }

    /**
     * Seeds a discovered bank account into the `bank-account` table with an incomplete
     * sort key that lacks the budget and account path segments.
     *
     * Builds the partition URI `noodle.ynabsync://<email>/account/<number>` and writes
     * an incomplete sort key `noodle.ynabsync://<userId>@app.ynab.com` (no path).
     * Discovery rows persist until manually completed by appending the path.
     */
    override suspend fun putDiscoveredAccount(
        email: String,
        number: String,
        userId: String,
    ) {
        val partition = "noodle.ynabsync://$email/account/$number"
        val sort = "noodle.ynabsync://$userId@app.ynab.com"
        put(partition, sort)
    }
}
