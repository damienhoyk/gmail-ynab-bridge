package noodle.tokenrefresher.infrastructure.persistence

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import noodle.dynamodb.DynamoDbSortRepository
import noodle.tokenrefresher.core.domain.RefreshableToken
import noodle.tokenrefresher.core.port.TokenRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

public class DynamoDbTokenRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    TokenRepository {
    public override val name: String = "token"

    public override val partitionKey: String = "id"
    public override val sortKey: String = "type"

    public override suspend fun findRefreshable(): List<RefreshableToken> =
        withContext(IO) {
            buildList {
                var startKey: Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue>? = null
                do {
                    val page =
                        client.scan {
                            it
                                .tableName(table)
                                .filterExpression("#t = :refresh")
                                .expressionAttributeNames(mapOf("#t" to "type"))
                                .expressionAttributeValues(mapOf(":refresh" to fromS("refresh")))
                                .apply { startKey?.let(::exclusiveStartKey) }
                        }
                    page.items().forEach { add(RefreshableToken(it["id"]!!.s(), it["value"]!!.s())) }
                    startKey = page.lastEvaluatedKey().ifEmpty { null }
                } while (startKey != null)
            }
        }

    public override suspend fun updateAccess(
        id: String,
        value: String,
    ) {
        update(id, "access") { put("value", fromS(value)) }
    }

    public override suspend fun updateRefresh(
        id: String,
        value: String,
    ) {
        update(id, "refresh") { put("value", fromS(value)) }
    }
}
