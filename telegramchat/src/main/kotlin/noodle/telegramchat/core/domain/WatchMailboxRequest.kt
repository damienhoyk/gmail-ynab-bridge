package noodle.telegramchat.core.domain

data class WatchMailboxRequest(
    val topicName: String,
    val labelIds: List<String>,
)
