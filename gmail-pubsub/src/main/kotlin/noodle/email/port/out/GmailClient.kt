package noodle.email.port.out

import noodle.email.domain.GmailHistory
import noodle.email.domain.GmailHistoryRequest

interface GmailClient {
    suspend fun getHistory(request: GmailHistoryRequest): GmailHistory
}
