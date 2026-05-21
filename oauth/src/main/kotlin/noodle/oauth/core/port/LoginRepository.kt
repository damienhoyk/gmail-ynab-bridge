package noodle.oauth.core.port

import noodle.oauth.core.domain.Login

interface LoginRepository {
    suspend fun putLogin(login: Login)
}
