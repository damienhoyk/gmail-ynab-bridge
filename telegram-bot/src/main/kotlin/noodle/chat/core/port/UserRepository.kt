package noodle.chat.core.port

import noodle.chat.core.domain.User

interface UserRepository {
    suspend fun putUser(user: User)

    suspend fun queryUser(id: String): List<User>
}
