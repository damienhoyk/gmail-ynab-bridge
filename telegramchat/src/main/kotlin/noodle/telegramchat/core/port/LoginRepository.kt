package noodle.telegramchat.core.port

import noodle.telegramchat.core.domain.Login

interface LoginRepository {
    suspend fun putLogin(login: Login)

    suspend fun getLogin(id: String): Login?
}
