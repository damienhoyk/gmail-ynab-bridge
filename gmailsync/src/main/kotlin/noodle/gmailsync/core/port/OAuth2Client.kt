package noodle.gmailsync.core.port

interface OAuth2Client {
    suspend fun getTokenInfo(token: String): String?
}
