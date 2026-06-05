package noodle.gmailsync.core.port

public interface LoginIdProvider {
    public suspend fun getTokenInfo(token: String): String?
}
