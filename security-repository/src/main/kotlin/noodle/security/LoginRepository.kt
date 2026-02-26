package noodle.security

import noodle.database.DynamoDbRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class LoginRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null
) : DynamoDbRepository() {

    override val name = "login"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "id"

}