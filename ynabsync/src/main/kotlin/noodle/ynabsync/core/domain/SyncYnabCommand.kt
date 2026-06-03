package noodle.ynabsync.core.domain

public data class SyncYnabCommand(
    public val destination: String,
    public val mailId: String,
    public val mailAddress: String,
    public val source: String,
)
