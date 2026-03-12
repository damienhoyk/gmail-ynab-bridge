package noodle.email.domain

data class SyncMailboxCommand(val emailAddress: String, val authorization: String, val state: Long)
