package noodle.security

import noodle.database.DynamoDbSortRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class TokenRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null
) : DynamoDbSortRepository() {

    override val name = "token"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "id"
    override val sortKey = "type"

}