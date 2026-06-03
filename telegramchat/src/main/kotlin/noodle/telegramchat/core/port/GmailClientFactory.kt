package noodle.telegramchat.core.port

public interface GmailClientFactory {
    public suspend fun create(loginId: String): GmailClient
}
