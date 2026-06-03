package noodle.ynabsync.core.port

public interface YnabClientFactory {
    public suspend fun create(loginId: String): YnabClient
}
