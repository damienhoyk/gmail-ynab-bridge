package noodle.gmailsync.core.domain

data class SyncMailboxCommand(val email: String?, val authorization: String?, val state: Long?) {
    val bearerToken: String
        get() = authorization?.substringAfter("Bearer ") ?: ""
}
