package noodle.dynamodb

import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class TestDynamoDbRepository(
    environment: String? = null,
) : DynamoDbRepository(environment) {
    override val client: DynamoDbClient = DynamoDbClient.create()
    override val name = "dynamodb"
    override val partitionKey = "partition"
}
