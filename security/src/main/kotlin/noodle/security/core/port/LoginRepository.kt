package noodle.security.core.port

import noodle.security.core.domain.Login

interface LoginRepository {
    suspend fun putLogin(login: Login)
}
