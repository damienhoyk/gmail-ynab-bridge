package noodle.gmailsync.core.port

interface GmailClient {
    suspend fun getAddedMessageIds(sinceHistoryId: Long): List<String>
}
