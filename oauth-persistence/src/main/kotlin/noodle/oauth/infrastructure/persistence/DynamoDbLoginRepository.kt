package noodle.oauth.infrastructure.persistence

import noodle.dynamodb.DynamoDbRepository
import noodle.oauth.core.domain.Login
import noodle.oauth.core.port.LoginRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

public class DynamoDbLoginRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbRepository(environment),
    LoginRepository {
    public override val name: String = "login"

    public override val partitionKey: String = "id"

    public override suspend fun putLogin(login: Login) {
        put(login.id) { put("userId", fromS(login.userId)) }
    }
}
