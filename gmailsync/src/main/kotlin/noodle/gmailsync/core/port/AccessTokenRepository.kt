package noodle.gmailsync.core.port

public interface AccessTokenRepository {
    public suspend fun getAccessToken(loginId: String): String?
}
