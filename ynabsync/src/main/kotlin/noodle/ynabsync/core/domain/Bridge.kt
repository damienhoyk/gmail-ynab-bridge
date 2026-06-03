package noodle.ynabsync.core.domain

public data class Bridge(
    val source: String,
    val destination: String,
    val accounts: Map<String, String>?,
)
