package io.github.samolego.ascendo_trainboard.api

import io.github.samolego.ascendo_trainboard.api.generated.models.Error

/**
 * Exception thrown when API returns an error response.
 * Contains parsed error details including timeout for rate limiting.
 */
class ApiException(
    val errorResponse: Error,
    val httpStatusCode: Int
) : Exception(errorResponse.error) {

    /**
     * The error code from the API (e.g., "INVALID_CREDENTIALS", "RATE_LIMIT", "BANNED")
     */
    val errorCode: String get() = errorResponse.code

    /**
     * Number of seconds to wait before retrying (null if not applicable)
     */
    val timeout: Long? get() = errorResponse.timeout

    /**
     * User-friendly error message
     */
    val errorMessage: String get() = errorResponse.error

    override fun toString(): String {
        return "ApiException(code=$errorCode, message=$errorMessage, timeout=$timeout, httpStatus=$httpStatusCode)"
    }
}
