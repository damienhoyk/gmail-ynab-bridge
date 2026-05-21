package noodle.telegramchat.infrastructure.persistence

import noodle.dynamodb.DynamoDbRepository
import noodle.telegramchat.core.domain.Mailbox
import noodle.telegramchat.core.port.MailboxRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN

class DynamoDbMailboxRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbRepository(environment), MailboxRepository {
    override val name = "mailbox"

    override val partitionKey = "address"

    override suspend fun updateMailbox(mailbox: Mailbox) {
        update(mailbox.address) {
            put("state", fromN(mailbox.state?.toString()))
        }
    }
}
