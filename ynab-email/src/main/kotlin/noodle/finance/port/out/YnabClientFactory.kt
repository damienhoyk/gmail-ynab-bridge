package noodle.finance.port.out

interface YnabClientFactory {
    suspend fun create(loginId: String): YnabClient
}
