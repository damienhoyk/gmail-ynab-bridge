package noodle.gmailsync.infrastructure.api

import noodle.gmailsync.core.domain.TokenInfoResponse
import noodle.gmailsync.core.port.GoogleTokenClient
import noodle.google.infrastructure.api.KtorGoogleAuthClient

class GmailTokenClientAdapter(private val googleAuthClient: KtorGoogleAuthClient) :
    GoogleTokenClient {
    override suspend fun getTokenInfo(token: String): TokenInfoResponse {
        val tokenInfo = googleAuthClient.getTokenInfo(token)
        return TokenInfoResponse(email = tokenInfo.email)
    }
}
