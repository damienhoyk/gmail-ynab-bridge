package noodle.user

import noodle.dynamodb.SortCrudRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class UserRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null
) : SortCrudRepository() {

    override val name = "user"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "id"
    override val sortKey = "loginId"

}