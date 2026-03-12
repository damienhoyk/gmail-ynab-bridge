package noodle.email.port.out

interface GmailClientFactory {
    suspend fun create(loginId: String): GmailClient
}
