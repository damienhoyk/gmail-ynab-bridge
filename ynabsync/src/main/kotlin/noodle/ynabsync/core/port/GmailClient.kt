package noodle.ynabsync.core.port

import noodle.gmailsync.core.domain.GmailMessageRequest
import noodle.ynabsync.core.domain.GmailMessage

interface GmailClient {
    suspend fun getMessage(request: GmailMessageRequest): GmailMessage
}
