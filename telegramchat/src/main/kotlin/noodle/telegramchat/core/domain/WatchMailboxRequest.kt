package noodle.telegramchat.core.domain

public data class WatchMailboxRequest(
    val topicName: String,
    val labelIds: List<String>,
)
