package noodle.security

import noodle.dynamodb.CrudRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class LoginRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null
) : CrudRepository() {

    override val name = "login"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "id"

}