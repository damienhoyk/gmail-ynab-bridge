package noodle.tokenrefresher.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import noodle.dynamodb.DynamoDbSortRepository
import noodle.tokenrefresher.core.domain.RefreshableToken
import noodle.tokenrefresher.core.port.TokenRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import java.time.Instant

public class DynamoDbTokenRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
    private val limit: Int = 25,
    private val bufferSeconds: Long = 900,
) : DynamoDbSortRepository(environment),
    TokenRepository {
    public override val name: String = "token"

    public override val partitionKey: String = "id"
    public override val sortKey: String = "type"

    private val log = LoggerFactory.getLogger(javaClass)

    public override fun findRefreshable(): Flow<List<RefreshableToken>> =
        scan {
            limit(limit)
            filterExpression("#t = :access AND (attribute_not_exists(#e) OR #e <= :threshold)")
            expressionAttributeNames(mapOf("#t" to "type", "#e" to "expiresAt"))
            expressionAttributeValues(
                mapOf(
                    ":access" to fromS("access"),
                    ":threshold" to fromN("${Instant.now().epochSecond + bufferSeconds}"),
                ),
            )
        }.map { page ->
            page.items().mapNotNull { accessItem ->
                val id = accessItem["id"]?.s() ?: return@mapNotNull null
                val refreshResponse = runBlocking { get(id, "refresh") }
                val refreshValue = refreshResponse.item()?.get("value")?.s()

                if (refreshValue.isNullOrBlank()) {
                    log.warn("Token [{}] has no refresh value; skipping", id)
                    return@mapNotNull null
                }

                RefreshableToken(id, refreshValue)
            }
        }

    public override suspend fun updateAccess(
        id: String,
        value: String,
        expiresIn: Long,
    ) {
        update(id, "access") {
            put("value", fromS(value))
            put("expiresAt", fromN("${Instant.now().epochSecond + expiresIn}"))
        }
    }

    public override suspend fun updateRefresh(
        id: String,
        value: String,
    ) {
        update(id, "refresh") { put("value", fromS(value)) }
    }
}
