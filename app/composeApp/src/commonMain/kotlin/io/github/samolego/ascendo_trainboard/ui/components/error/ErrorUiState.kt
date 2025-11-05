package io.github.samolego.ascendo_trainboard.ui.components.error

import io.github.samolego.ascendo_trainboard.api.ApiException
import kotlinx.serialization.SerializationException
import okio.IOException


data class ErrorUiState(
    val message: String,
)

fun Throwable.toErrorUiState(): ErrorUiState {
    return when (this) {
        is ApiException -> ErrorUiState(
            message = this.errorMessage,
        )
        is IOException -> ErrorUiState(
            message = "Network error. Please check your connection.",
        )
        is SerializationException
        -> ErrorUiState(
            message = "Unable to process server response.",
        )
        else -> ErrorUiState(
            message = this.message ?: "An unexpected error occurred.",
        )
    }
}
