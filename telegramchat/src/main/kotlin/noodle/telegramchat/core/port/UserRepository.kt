package noodle.telegramchat.core.port

import noodle.telegramchat.core.domain.User

interface UserRepository {
    suspend fun putUser(user: User)

    suspend fun queryUser(id: String): List<User>
}
