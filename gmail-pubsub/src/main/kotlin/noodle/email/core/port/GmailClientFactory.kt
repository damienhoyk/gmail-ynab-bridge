package noodle.email.core.port

interface GmailClientFactory {
    suspend fun create(loginId: String): GmailClient
}
