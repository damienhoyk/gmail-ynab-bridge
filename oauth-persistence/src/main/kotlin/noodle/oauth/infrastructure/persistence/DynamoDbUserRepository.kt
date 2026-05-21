package noodle.oauth.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.oauth.core.domain.User
import noodle.oauth.core.port.UserRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class DynamoDbUserRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment), UserRepository {
    override val name = "user"

    override val partitionKey = "id"
    override val sortKey = "loginId"

    override suspend fun putUser(user: User) {
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
}
