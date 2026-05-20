package noodle.finance.core.port

interface YnabClientFactory {
    suspend fun create(loginId: String): YnabClient
}
