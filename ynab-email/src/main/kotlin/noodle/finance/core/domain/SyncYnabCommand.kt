package noodle.finance.core.domain

data class SyncYnabCommand(
    val destination: String,
    val mailId: String,
    val mailAddress: String,
    val source: String,
)
