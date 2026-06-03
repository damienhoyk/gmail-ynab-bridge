package noodle.telegramchat.core.port

import noodle.telegramchat.core.domain.User

public interface UserRepository {
    public suspend fun putUser(user: User)

    public suspend fun queryUser(id: String): List<User>
}
