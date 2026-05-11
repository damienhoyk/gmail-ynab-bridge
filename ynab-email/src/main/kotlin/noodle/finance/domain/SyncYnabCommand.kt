package noodle.finance.domain

data class SyncYnabCommand(
    val destination: String,
    val mailId: String,
    val mailAddress: String,
    val source: String,
)
