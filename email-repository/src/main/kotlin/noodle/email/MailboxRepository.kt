package noodle.email

import noodle.dynamodb.CrudRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class MailboxRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null
): CrudRepository() {

    override val name = "mailbox"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "address"

}