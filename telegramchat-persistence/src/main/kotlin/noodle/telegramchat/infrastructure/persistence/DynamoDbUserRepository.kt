package noodle.telegramchat.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.telegramchat.core.domain.User
import noodle.telegramchat.core.port.UserRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

public class DynamoDbUserRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    UserRepository {
    override val name: String = "user"

    override val partitionKey: String = "id"
    override val sortKey: String = "loginId"

    override suspend fun putUser(user: User) {
        put(user.id, user.loginId)
    }

    override suspend fun queryLogins(id: String): List<URI> =
        query(id)
            .items()
            .mapNotNull {
                it["loginId"]?.s()
            }.map(::URI)
}
