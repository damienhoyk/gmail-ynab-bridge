package noodle.chat.domain

data class RespondChatCommand(val telegramUserId: String, val message: String, val chatId: String)
