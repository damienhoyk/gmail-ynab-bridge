package noodle.email

import noodle.database.DynamoDbSortRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class MailRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null
) : DynamoDbSortRepository() {

    override val name = "mail"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "address"
    override val sortKey = "mailId"

}