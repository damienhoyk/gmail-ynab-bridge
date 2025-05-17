package noodle.home.security

interface AccessTokenProvider {

    fun getToken(id: String? = null): String
    fun getNewToken(id: String? = null): String

}