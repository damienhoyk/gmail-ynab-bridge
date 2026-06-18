package noodle.telegramchat.core.port

import noodle.telegramchat.core.domain.User
import java.net.URI

public interface UserRepository {
    public suspend fun putUser(user: User)

    public suspend fun queryLogins(id: String): List<URI>
}
