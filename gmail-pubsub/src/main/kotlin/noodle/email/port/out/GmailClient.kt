package noodle.email.port.out

interface GmailClient {
    suspend fun getAddedMessageIds(sinceHistoryId: Long): List<String>
}
