package noodle.finance.port.out

import noodle.email.domain.GmailMessageRequest
import noodle.finance.domain.GmailMessage

interface GmailClient {
    suspend fun getMessage(request: GmailMessageRequest): GmailMessage
}
