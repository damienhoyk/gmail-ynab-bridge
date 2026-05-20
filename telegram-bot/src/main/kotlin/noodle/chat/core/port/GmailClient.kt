package noodle.chat.core.port

import noodle.chat.core.domain.GmailProfile
import noodle.chat.core.domain.GmailWatch
import noodle.email.domain.GmailWatchRequest

interface GmailClient {
    suspend fun getProfile(): GmailProfile?

    suspend fun getLabelId(labelName: String): String?

    suspend fun postWatch(request: GmailWatchRequest): GmailWatch
}
