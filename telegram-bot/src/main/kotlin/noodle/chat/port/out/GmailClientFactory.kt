package noodle.chat.port.out

interface GmailClientFactory {
    suspend fun create(loginId: String): GmailClient
}
