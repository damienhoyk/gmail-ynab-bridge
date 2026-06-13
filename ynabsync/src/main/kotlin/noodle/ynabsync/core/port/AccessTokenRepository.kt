package noodle.ynabsync.core.port

public interface AccessTokenRepository {
    public suspend fun getAccessToken(loginId: String): String?
}
