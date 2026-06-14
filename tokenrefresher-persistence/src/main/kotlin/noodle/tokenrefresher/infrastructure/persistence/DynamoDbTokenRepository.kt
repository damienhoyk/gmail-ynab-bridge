package noodle.tokenrefresher.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import noodle.dynamodb.DynamoDbSortRepository
import noodle.tokenrefresher.core.domain.RefreshableToken
import noodle.tokenrefresher.core.port.TokenRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

public class DynamoDbTokenRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
    private val limit: Int = 25,
) : DynamoDbSortRepository(environment),
    TokenRepository {
    public override val name: String = "token"

    public override val partitionKey: String = "id"
    public override val sortKey: String = "type"

    private val log = LoggerFactory.getLogger(javaClass)

    public override fun findRefreshable(): Flow<List<RefreshableToken>> =
        scan {
            limit(limit)
            filterExpression("#t = :t")
            expressionAttributeNames(mapOf("#t" to "type"))
            expressionAttributeValues(mapOf(":t" to fromS("refresh")))
        }.map { page ->
            val (refreshable, malformed) =
                page.items().partition { it["value"]?.s() != null }

            malformed.forEach {
                log.warn("Token [{}] has no refresh value; skipping", it["id"]?.s())
            }

            refreshable.map { RefreshableToken(it["id"]!!.s(), it["value"]!!.s()) }
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
