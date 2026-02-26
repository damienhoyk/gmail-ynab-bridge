package noodle.email

import noodle.dynamodb.SortCrudRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class MailRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null
) : SortCrudRepository() {

    override val name = "mail"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "address"
    override val sortKey = "mailId"

}