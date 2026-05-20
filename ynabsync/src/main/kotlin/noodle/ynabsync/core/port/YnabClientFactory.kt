package noodle.ynabsync.core.port

interface YnabClientFactory {
    suspend fun create(loginId: String): YnabClient
}
