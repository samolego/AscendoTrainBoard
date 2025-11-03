
# Error

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **error** | **kotlin.String** | Error message description |  |
| **code** | **kotlin.String** | Error code. Common values include: - INVALID_CREDENTIALS: Invalid username or password - RATE_LIMIT: Too many login attempts, must wait before retry - BANNED: IP banned due to excessive failed login attempts - NOT_AUTHENTICATED: Authentication required - FORBIDDEN: Insufficient permissions - NOT_FOUND: Resource not found - INVALID_USERNAME, INVALID_PASSWORD, etc.: Validation errors  |  |
| **timeout** | **kotlin.Long** | Number of seconds to wait before retrying. Present for authentication-related errors: - For INVALID_CREDENTIALS: wait time before next attempt (3s × failed attempt count, or 7200s if banned) - For RATE_LIMIT: seconds until next login attempt is allowed - For BANNED: seconds until ban expires (7200 for 2 hours) - null for other error types  |  [optional] |



