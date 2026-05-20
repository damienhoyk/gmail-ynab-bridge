package noodle.security.infrastructure.persistence

import noodle.database.DynamoDbRepository
import noodle.security.core.domain.Login
import noodle.security.core.port.LoginRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class DynamoDbLoginRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbRepository(), LoginRepository, noodle.telegramchat.core.port.LoginRepository {
    override val name = "login"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "id"

    override suspend fun putLogin(login: Login) {
        put(login.id) { put("userId", fromS(login.userId)) }
    }

    override suspend fun putLogin(login: noodle.telegramchat.core.domain.Login) {
        put(login.id) { put("userId", fromS(login.userId)) }
    }

    override suspend fun getLogin(id: String): noodle.telegramchat.core.domain.Login? =
        get(id)?.let {
            val item = it.item()
            val id = item["id"]?.s()!!
            val userId = item["userId"]?.s()
            noodle.telegramchat.core.domain.Login(id, userId)
        }
}
