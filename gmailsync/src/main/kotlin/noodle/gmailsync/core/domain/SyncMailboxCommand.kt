package noodle.gmailsync.core.domain

public data class SyncMailboxCommand(
    public val email: String?,
    public val authorization: String?,
    public val state: Long?,
) {
    public val bearerToken: String
        get() = authorization?.substringAfter("Bearer ") ?: ""
}
