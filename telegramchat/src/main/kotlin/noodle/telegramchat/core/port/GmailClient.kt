package noodle.telegramchat.core.port

import noodle.telegramchat.core.domain.GmailProfile
import noodle.telegramchat.core.domain.GmailWatch
import noodle.telegramchat.core.domain.WatchMailboxRequest

public interface GmailClient {
    public suspend fun getProfile(): GmailProfile?

    public suspend fun getLabelId(labelName: String): String?

    public suspend fun postWatch(request: WatchMailboxRequest): GmailWatch
}
