package noodle.chat.core.port

import noodle.chat.core.domain.Login

interface LoginRepository {
    suspend fun putLogin(login: Login)

    suspend fun getLogin(id: String): Login?
}
