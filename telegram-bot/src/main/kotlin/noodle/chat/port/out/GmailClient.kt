package noodle.chat.port.out

import noodle.chat.domain.GmailLabel
import noodle.chat.domain.GmailProfile
import noodle.chat.domain.GmailWatch
import noodle.email.domain.GmailWatchRequest

interface GmailClient {
    suspend fun getProfile(): GmailProfile?

    suspend fun getLabels(): GmailLabel.List?

    suspend fun postWatch(request: GmailWatchRequest): GmailWatch
}
