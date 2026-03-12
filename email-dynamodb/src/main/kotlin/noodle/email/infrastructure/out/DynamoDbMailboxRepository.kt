package noodle.email.infrastructure.out

import noodle.database.DynamoDbRepository
import noodle.email.domain.Mailbox
import noodle.email.port.out.MailboxRepository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN

class DynamoDbMailboxRepository(
    override val client: DynamoDbClient = DynamoDbClient.create(),
    environment: String? = null,
) : DynamoDbRepository(), MailboxRepository, noodle.chat.port.out.MailboxRepository {
    override val name = "mailbox"
    override val table = environment?.let { "$name-$it" } ?: name

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

    override suspend fun updateMailbox(mailbox: noodle.chat.domain.Mailbox) {
        update(mailbox.address) {
            put("state", fromN(mailbox.state?.toString()))
        }
    }
}
