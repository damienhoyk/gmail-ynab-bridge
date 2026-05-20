package noodle.gmailsync.core.port

interface GmailClientFactory {
    suspend fun create(loginId: String): GmailClient
}
