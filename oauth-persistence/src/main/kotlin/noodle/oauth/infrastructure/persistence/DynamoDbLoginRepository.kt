package noodle.oauth.infrastructure.persistence

import noodle.dynamodb.DynamoDbRepository
import noodle.oauth.core.domain.Login
import noodle.oauth.core.port.LoginRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class DynamoDbLoginRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbRepository(), LoginRepository {
    override val name = "login"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "id"

    override suspend fun putLogin(login: Login) {
        put(login.id) { put("userId", fromS(login.userId)) }
    }
}
