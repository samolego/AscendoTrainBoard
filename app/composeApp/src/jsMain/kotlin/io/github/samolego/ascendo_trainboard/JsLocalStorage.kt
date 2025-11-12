package io.github.samolego.ascendo_trainboard

import io.github.samolego.ascendo_trainboard.api.generated.models.LoginResponse
import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json

class JsLocalStorage : PlatformStorage {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadProperty(key: String): String? {
        return localStorage.getItem(key)
    }

    override suspend fun saveProperty(key: String, value: String) {
        localStorage.setItem(key, value)
    }

    override suspend fun saveLoginInfo(info: LoginResponse) {
        val jsonString = json.encodeToString(info)
        localStorage.setItem(PlatformStorage.KEY_LOGIN_INFO, jsonString)
    }

    override suspend fun loadLoginInfo(): LoginResponse? {
        val jsonString = localStorage.getItem(PlatformStorage.KEY_LOGIN_INFO) ?: return null
        return try {
            json.decodeFromString<LoginResponse>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
}
