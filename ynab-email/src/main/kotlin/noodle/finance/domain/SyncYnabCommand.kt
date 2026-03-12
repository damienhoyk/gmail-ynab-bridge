package noodle.finance.domain

data class SyncYnabCommand(
    val destination: String? = null,
    val source: String? = null,
) {
    val mailAddress get() = source?.substringAfter(":")
    val mailId get() = source?.substringBefore(":")
}
