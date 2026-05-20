package noodle.finance.core.port

import noodle.email.domain.GmailMessageRequest
import noodle.finance.core.domain.GmailMessage

interface GmailClient {
    suspend fun getMessage(request: GmailMessageRequest): GmailMessage
}
