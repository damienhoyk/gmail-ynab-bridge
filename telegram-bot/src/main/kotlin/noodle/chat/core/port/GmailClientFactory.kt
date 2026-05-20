package noodle.chat.core.port

interface GmailClientFactory {
    suspend fun create(loginId: String): GmailClient
}
