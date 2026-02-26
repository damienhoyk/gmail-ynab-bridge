package noodle.email

import noodle.dynamodb.SortCrudRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class BridgeRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null
) : SortCrudRepository() {

    override val name = "bridge"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "source"
    override val sortKey = "destination"

}