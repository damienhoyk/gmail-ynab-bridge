package noodle.gmailsync.core.port

import noodle.gmailsync.core.domain.Mailbox

public interface MailboxRepository {
    public suspend fun getMailbox(address: String): Mailbox

    public suspend fun updateMailbox(mailbox: Mailbox)
}
