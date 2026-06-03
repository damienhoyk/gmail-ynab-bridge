package noodle.oauth.core.port

import noodle.oauth.core.domain.User

public interface UserRepository {
    public suspend fun putUser(user: User)
}
