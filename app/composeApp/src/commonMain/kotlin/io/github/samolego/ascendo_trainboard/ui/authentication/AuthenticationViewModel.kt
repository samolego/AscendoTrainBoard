@file:OptIn(ExperimentalTime::class)

package io.github.samolego.ascendo_trainboard.ui.authentication

import androidx.lifecycle.ViewModel
import io.github.samolego.ascendo_trainboard.api.ApiException
import io.github.samolego.ascendo_trainboard.api.AscendoApi
import io.github.samolego.ascendo_trainboard.ui.components.error.ErrorUiState
import io.github.samolego.ascendo_trainboard.ui.components.error.toErrorUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock.System.now
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class AuthenticationState(
    val isAuthenticated: Boolean = false,
    val username: String = "",
    val timeoutUntil: Instant? = null,
    val error: ErrorUiState? = null,
    val isLoading: Boolean = false
)

class AuthenticationViewModel(
    private val api: AscendoApi
) : ViewModel() {

    private val _state = MutableStateFlow(
        AuthenticationState(
            isAuthenticated = api.isAuthenticated(),
            username = api.username ?: ""
        )
    )
    val state: StateFlow<AuthenticationState> = _state.asStateFlow()

    suspend fun login(username: String, password: String) {
        _state.update { it.copy(isLoading = true, error = null) }

        api.login(username, password)
            .onSuccess {
                _state.update {
                    it.copy(
                        isAuthenticated = true,
                        username = username,
                        timeoutUntil = null,
                        error = null,
                        isLoading = false
                    )
                }
            }
            .onFailure { throwable ->
                _state.update {
                    val nextAttempt = (throwable as? ApiException)?.timeout?.let {
                        now() + it.seconds
                    }
                    it.copy(
                        isLoading = false,
                        timeoutUntil = nextAttempt,
                        error = throwable.toErrorUiState()
                    )
                }
            }
    }

    suspend fun register(username: String, password: String) {
        _state.update { it.copy(isLoading = true, error = null) }

        api.register(username, password)
            .onSuccess {
                login(username, password)
            }
            .onFailure { throwable ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.toErrorUiState()
                    )
                }
            }
    }

    suspend fun logout() {
        api.logout()
            .onFailure { throwable ->
                _state.update {
                    it.copy(error = throwable.toErrorUiState())
                }
            }

        _state.update {
            it.copy(
                isAuthenticated = false,
                username = "",
                timeoutUntil = null
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearTimeout() {
        _state.update { it.copy(timeoutUntil = null) }
    }
}
