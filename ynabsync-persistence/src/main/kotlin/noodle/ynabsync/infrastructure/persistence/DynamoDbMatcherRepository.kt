package noodle.ynabsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.ynabsync.core.domain.TransactionMatcher
import noodle.ynabsync.core.domain.TransactionMatcher.RegexGroup
import noodle.ynabsync.core.port.MatcherRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

public class DynamoDbMatcherRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    MatcherRepository {
    override val name: String = "gmail-ynab-bridge-matcher"

    override val partitionKey: String = "source"
    override val sortKey: String = "mode"

    override suspend fun queryMatcher(source: String): List<TransactionMatcher> =
        query(source).items().mapNotNull {
            val datePattern = it["datePattern"]?.s()
            val pattern = it["pattern"]?.s()?.toRegex()
            val order =
                it["order"]
                    ?.l()
                    ?.map(AttributeValue::s)
                    ?.map(RegexGroup::valueOf)
                    ?.toSet()

            if (datePattern.isNullOrBlank() || pattern == null || order.isNullOrEmpty()) {
                null
            } else {
                TransactionMatcher(pattern, order = order, inputDatePattern = datePattern)
            }
        }
}
