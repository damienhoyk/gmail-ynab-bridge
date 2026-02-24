package noodle.home.security

interface AccessTokenProvider {

    suspend fun getToken(id: String? = null): String
    suspend fun getNewToken(id: String? = null): String

}