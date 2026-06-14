package noodle.tokenrefresher.infrastructure.persistence

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import noodle.dynamodb.DynamoDbSortRepository
import noodle.tokenrefresher.core.domain.RefreshableToken
import noodle.tokenrefresher.core.port.TokenRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.dynamodb.model.ScanResponse

public class DynamoDbTokenRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
    private val pageLimit: Int = 25,
) : DynamoDbSortRepository(environment),
    TokenRepository {
    public override val name: String = "token"

    public override val partitionKey: String = "id"
    public override val sortKey: String = "type"

    private val log = LoggerFactory.getLogger(javaClass)

    public override fun findRefreshable(): Flow<List<RefreshableToken>> =
        generateSequence(::scan) { previous ->
            previous.lastEvaluatedKey().ifEmpty { null }?.let(::scan)
        }.asFlow()
            .map { page ->
                val (refreshable, malformed) =
                    page.items().partition { it["value"]?.s() != null }

                malformed.forEach {
                    log.warn("Token [{}] has no refresh value; skipping", it["id"]?.s())
                }

                refreshable.map { RefreshableToken(it["id"]!!.s(), it["value"]!!.s()) }
            }.flowOn(IO)

    private fun scan(startKey: Map<String, AttributeValue>? = null): ScanResponse =
        client.scan {
            it
                .tableName(table)
                .limit(pageLimit)
                .filterExpression("#t = :refresh")
                .expressionAttributeNames(mapOf("#t" to "type"))
                .expressionAttributeValues(mapOf(":refresh" to fromS("refresh")))
                .apply { startKey?.let(::exclusiveStartKey) }
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
