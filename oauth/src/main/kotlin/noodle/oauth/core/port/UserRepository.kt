package noodle.oauth.core.port

import noodle.oauth.core.domain.User

interface UserRepository {
    suspend fun putUser(user: User)
}
