package noodle.home.security

interface TokenStore {

    fun getRefreshToken(id: String): String?
    fun getAccessToken(id: String): String?
    fun storeAccessToken(id: String, accessToken: String)
    fun storeRefreshToken(id: String, refreshToken: String)

}