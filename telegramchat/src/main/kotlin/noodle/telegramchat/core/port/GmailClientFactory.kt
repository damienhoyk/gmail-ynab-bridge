package noodle.telegramchat.core.port

interface GmailClientFactory {
    suspend fun create(loginId: String): GmailClient
}
