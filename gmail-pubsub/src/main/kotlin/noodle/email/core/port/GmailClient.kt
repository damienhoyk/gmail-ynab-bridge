package noodle.email.core.port

interface GmailClient {
    suspend fun getAddedMessageIds(sinceHistoryId: Long): List<String>
}
