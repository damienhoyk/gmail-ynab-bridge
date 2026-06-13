package noodle.telegramchat.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.telegramchat.core.port.AccessTokenRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

public class DynamoDbAccessTokenRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    AccessTokenRepository {
    public override val name: String = "token"

    public override val partitionKey: String = "id"
    public override val sortKey: String = "type"

    public override suspend fun getAccessToken(loginId: String): String? = get(loginId, "access").item()["value"]?.s()
}
