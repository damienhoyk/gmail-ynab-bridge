package noodle.oauth.infrastructure.persistence

import noodle.dynamodb.DynamoDbSortRepository
import noodle.oauth.core.domain.User
import noodle.oauth.core.port.UserRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

public class DynamoDbUserRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbSortRepository(environment),
    UserRepository {
    public override val name: String = "user"

    public override val partitionKey: String = "id"
    public override val sortKey: String = "loginId"

    public override suspend fun putUser(user: User) {
        put(user.id, user.loginId)
    }

    public suspend fun getUser(
        id: String,
        loginId: String,
    ): User =
        get(id, loginId).item().let {
            val id = it["id"]?.s()!!
            val loginId = it["loginId"]?.s()!!
            User(id, loginId)
        }
}
