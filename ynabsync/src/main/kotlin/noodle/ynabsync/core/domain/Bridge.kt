package noodle.ynabsync.core.domain

public data class Bridge(
    val bankAccount: String,
    val destination: YnabUrn,
)
