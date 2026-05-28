package noodle.gmailsync.core.port

import noodle.gmailsync.core.domain.TokenInfoResponse

interface OAuth2Client {
    suspend fun getTokenInfo(token: String): TokenInfoResponse
}
