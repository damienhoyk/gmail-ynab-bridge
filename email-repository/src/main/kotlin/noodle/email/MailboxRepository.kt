package noodle.email

import noodle.database.DynamoDbRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class MailboxRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null
): DynamoDbRepository() {

    override val name = "mailbox"
    override val table = environment?.let { "$name-$it" } ?: name

    override val partitionKey = "address"

}