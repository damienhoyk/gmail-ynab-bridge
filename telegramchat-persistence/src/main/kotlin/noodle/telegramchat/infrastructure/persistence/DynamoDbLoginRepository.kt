package noodle.telegramchat.infrastructure.persistence

import noodle.dynamodb.DynamoDbRepository
import noodle.telegramchat.core.domain.Login
import noodle.telegramchat.core.port.LoginRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

public class DynamoDbLoginRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbRepository(environment),
    LoginRepository {
    override val name: String = "login"

    override val partitionKey: String = "id"

    override suspend fun putLogin(login: Login) {
        put(login.id) { put("userId", fromS(login.userId)) }
    }

    override suspend fun getLogin(id: String): Login? =
        get(id)?.let {
            val item = it.item()
            val id = item["id"]?.s()!!
            val userId = item["userId"]?.s()
            Login(id, userId)
        }
}
