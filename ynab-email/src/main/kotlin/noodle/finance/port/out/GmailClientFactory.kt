package noodle.finance.port.out

interface GmailClientFactory {
    suspend fun create(loginId: String): GmailClient
}
