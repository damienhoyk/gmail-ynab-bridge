package noodle.ynabsync.core.port

interface GmailClientFactory {
    suspend fun create(loginId: String): GmailClient
}
