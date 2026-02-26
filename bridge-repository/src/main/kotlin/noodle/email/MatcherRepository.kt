package noodle.email

import noodle.database.DynamoDbSortRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class MatcherRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null
) : DynamoDbSortRepository() {

    override val name = "gmail-ynab-bridge-matcher"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "source"
    override val sortKey = "mode"

}