package noodle.chat.port.out

import noodle.chat.domain.GmailProfile
import noodle.chat.domain.GmailWatch
import noodle.email.domain.GmailWatchRequest

interface GmailClient {
    suspend fun getProfile(): GmailProfile?

    suspend fun getLabels(): Map<String, String>

    suspend fun postWatch(request: GmailWatchRequest): GmailWatch
}
