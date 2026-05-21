package noodle.gmailsync.infrastructure.persistence

import noodle.dynamodb.DynamoDbRepository
import noodle.gmailsync.core.domain.Mailbox
import noodle.gmailsync.core.port.MailboxRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN

class DynamoDbMailboxRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbRepository(environment), MailboxRepository {
    override val name = "mailbox"

    override val partitionKey = "address"

    override suspend fun putMailbox(mailbox: Mailbox) {
        put(mailbox.address) {
            put("state", fromN(mailbox.state?.toString()))
        }
    }

    override suspend fun getMailbox(address: String): Mailbox {
        val item = get(address).item()
        val address = item["address"]?.s()!!
        val state = item["state"]?.n()?.toLong()
        return Mailbox(address, state)
    }
}
