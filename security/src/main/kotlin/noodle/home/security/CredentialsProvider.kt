package noodle.home.security

interface CredentialsProvider {
    suspend fun getClientId(): String?
    suspend fun getClientSecret(): String?
    suspend fun load()
}