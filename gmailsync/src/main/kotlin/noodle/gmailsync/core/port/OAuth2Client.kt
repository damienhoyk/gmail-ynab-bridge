package noodle.gmailsync.core.port

public interface OAuth2Client {
    public suspend fun getTokenInfo(token: String): String?
}
