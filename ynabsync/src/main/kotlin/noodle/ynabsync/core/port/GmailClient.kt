package noodle.ynabsync.core.port

import noodle.ynabsync.core.domain.GmailMessage
import noodle.ynabsync.core.domain.MailMessageRequest

interface GmailClient {
    suspend fun getMessage(request: MailMessageRequest): GmailMessage
}
