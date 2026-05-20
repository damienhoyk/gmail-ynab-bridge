package noodle.telegramchat.core.port

import noodle.telegramchat.core.domain.GmailProfile
import noodle.telegramchat.core.domain.GmailWatch
import noodle.telegramchat.core.domain.WatchMailboxRequest

interface GmailClient {
    suspend fun getProfile(): GmailProfile?

    suspend fun getLabelId(labelName: String): String?

    suspend fun postWatch(request: WatchMailboxRequest): GmailWatch
}
