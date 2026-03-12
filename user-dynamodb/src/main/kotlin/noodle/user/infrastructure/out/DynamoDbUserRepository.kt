package noodle.user.infrastructure.out

import noodle.database.DynamoDbSortRepository
import noodle.security.domain.User
import noodle.security.port.out.UserRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class DynamoDbUserRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(), UserRepository, noodle.chat.port.out.UserRepository {
    override val name = "user"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "id"
    override val sortKey = "loginId"

    override suspend fun putUser(user: User) {
        put(user.id, user.loginId)
    }

    override suspend fun putUser(user: noodle.chat.domain.User) {
        put(user.id, user.loginId)
    }

    suspend fun getUser(
        id: String,
        loginId: String,
    ) = get(id, loginId).item().let {
        val id = it["id"]?.s()!!
        val loginId = it["loginId"]?.s()!!
        User(id, loginId)
    }

    override suspend fun queryUser(id: String) =
        query(id).items().map {
            val id = it["id"]?.s()!!
            val loginId = it["loginId"]?.s()!!
            noodle.chat.domain.User(id, loginId)
        }
}
