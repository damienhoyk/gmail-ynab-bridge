package noodle.gmailsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbRepository
import noodle.gmailsync.core.port.LoginRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

public class DynamoDbLoginRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbRepository(environment),
    LoginRepository {
    override val name: String = "login"

    override val partitionKey: String = "id"

    override suspend fun resolve(email: String): String? {
        val handle = "noodle.oauth://$email"
        return get(handle).let {
            val item = it.item()
            item["userId"]?.s()
        }
    }
}
