package noodle.telegramchat.core.port

import noodle.gmailsync.core.domain.GmailWatchRequest
import noodle.telegramchat.core.domain.GmailProfile
import noodle.telegramchat.core.domain.GmailWatch

interface GmailClient {
    suspend fun getProfile(): GmailProfile?

    suspend fun getLabelId(labelName: String): String?

    suspend fun postWatch(request: GmailWatchRequest): GmailWatch
}
