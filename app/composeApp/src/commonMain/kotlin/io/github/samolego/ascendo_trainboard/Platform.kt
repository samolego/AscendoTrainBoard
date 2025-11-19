package io.github.samolego.ascendo_trainboard

import io.github.samolego.ascendo_trainboard.api.generated.models.LoginResponse

interface Platform {
    val name: String
    val storage: PlatformStorage

    fun baseUrl(): String
}

expect fun getPlatform(): Platform

interface PlatformStorage {
    suspend fun loadProperty(key: String): String?
    suspend fun saveProperty(key: String, value: String)
    suspend fun saveLoginInfo(info: LoginResponse)
    suspend fun loadLoginInfo(): LoginResponse?


    companion object {
        protected const val KEY_LOGIN_INFO = "login_info"
    }
}
