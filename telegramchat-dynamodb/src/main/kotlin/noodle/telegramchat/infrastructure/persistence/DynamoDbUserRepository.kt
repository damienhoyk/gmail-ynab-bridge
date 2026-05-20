package noodle.telegramchat.infrastructure.persistence

import noodle.database.DynamoDbSortRepository
import noodle.telegramchat.core.domain.User
import noodle.telegramchat.core.port.UserRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class DynamoDbUserRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(), UserRepository {
    override val name = "user"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "id"
    override val sortKey = "loginId"

    override suspend fun putUser(user: User) {
        put(user.id, user.loginId)
    }

    override suspend fun queryUser(id: String) =
        query(id).items().map {
            val id = it["id"]?.s()!!
            val loginId = it["loginId"]?.s()!!
            User(id, loginId)
        }
}
