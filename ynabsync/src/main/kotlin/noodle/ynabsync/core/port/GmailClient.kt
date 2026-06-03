package noodle.ynabsync.core.port

import noodle.ynabsync.core.domain.GmailMessage
import noodle.ynabsync.core.domain.MailMessageRequest

public interface GmailClient {
    public suspend fun getMessage(request: MailMessageRequest): GmailMessage
}
