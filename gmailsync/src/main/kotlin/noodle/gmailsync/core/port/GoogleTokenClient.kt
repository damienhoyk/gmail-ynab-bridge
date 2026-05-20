package noodle.gmailsync.core.port

import noodle.gmailsync.core.domain.TokenInfoResponse

interface GoogleTokenClient {
    suspend fun getTokenInfo(token: String): TokenInfoResponse
}
