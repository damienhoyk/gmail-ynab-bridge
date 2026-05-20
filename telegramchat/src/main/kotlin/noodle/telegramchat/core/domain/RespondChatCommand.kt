package noodle.telegramchat.core.domain

data class RespondChatCommand(
    val telegramUserId: String?,
    val text: String?,
    val chatId: String?,
) {
    val authority: String? get() = telegramUserId?.let { "$it@web.telegram.org" }
}
