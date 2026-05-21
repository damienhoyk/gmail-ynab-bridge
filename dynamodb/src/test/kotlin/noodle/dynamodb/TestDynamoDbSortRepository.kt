package noodle.dynamodb

import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class TestDynamoDbSortRepository(
    environment: String? = null,
) : DynamoDbSortRepository(environment) {
    override val client: DynamoDbClient = DynamoDbClient.create()
    override val name = "dynamodbsort"
    override val partitionKey = "partition"
    override val sortKey = "sort"
}
