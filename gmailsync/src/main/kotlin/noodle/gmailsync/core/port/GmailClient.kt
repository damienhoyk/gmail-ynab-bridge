package noodle.gmailsync.core.port

public interface GmailClient {
    public suspend fun getAddedMessageIds(sinceHistoryId: Long): List<String>
}
