package noodle.gmailsync.core.port

public interface GmailClientFactory {
    public suspend fun create(loginId: String): GmailClient
}
