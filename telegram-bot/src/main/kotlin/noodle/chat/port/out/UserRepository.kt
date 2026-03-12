package noodle.chat.port.out

import noodle.chat.domain.User

interface UserRepository {
    suspend fun putUser(user: User)

    suspend fun queryUser(id: String): List<User>
}
