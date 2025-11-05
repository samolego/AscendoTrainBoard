package io.github.samolego.ascendo_trainboard

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.samolego.ascendo_trainboard.api.generated.models.LoginResponse
import kotlinx.serialization.json.Json

class AndroidStorage(context: Context) : PlatformStorage {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ascendo_trainboard_prefs",
        Context.MODE_PRIVATE
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadProperty(key: String): String? {
        return prefs.getString(key, null)
    }

    override suspend fun saveProperty(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    override suspend fun saveLoginInfo(info: LoginResponse) {
        val jsonString = json.encodeToString(info)
        prefs.edit { putString(PlatformStorage.KEY_LOGIN_INFO, jsonString) }
    }

    override suspend fun loadLoginInfo(): LoginResponse? {
        val jsonString = prefs.getString(PlatformStorage.KEY_LOGIN_INFO, null) ?: return null
        return try {
            json.decodeFromString<LoginResponse>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
}
