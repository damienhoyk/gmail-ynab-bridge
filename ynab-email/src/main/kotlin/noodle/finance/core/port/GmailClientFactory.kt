package noodle.finance.core.port

interface GmailClientFactory {
    suspend fun create(loginId: String): GmailClient
}
