package io.github.samolego.ascendo_trainboard.ui.authentication

import androidx.lifecycle.ViewModel
import io.github.samolego.ascendo_trainboard.api.AscendoApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthenticationViewModel(
    private val api: AscendoApi
) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow(api.isAuthenticated())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _username = MutableStateFlow(api.username ?: "")
    val username: StateFlow<String> = _username.asStateFlow()

    suspend fun login(username: String, password: String): Boolean {
        return try {
            api.login(username, password)
            _isAuthenticated.value = api.isAuthenticated()
            _username.value = api.username ?: username
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun register(username: String, password: String): Boolean {
        return try {
            api.register(username, password)
            api.login(username, password)
            _isAuthenticated.value = api.isAuthenticated()
            _username.value = api.username ?: username
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun logout() {
        api.logout()
        _username.value = ""
        _isAuthenticated.value = false
    }
}
