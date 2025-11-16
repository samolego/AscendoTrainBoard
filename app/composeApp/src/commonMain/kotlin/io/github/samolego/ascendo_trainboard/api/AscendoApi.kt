package io.github.samolego.ascendo_trainboard.api

import io.github.samolego.ascendo_trainboard.api.generated.models.CreateProblemRequest
import io.github.samolego.ascendo_trainboard.api.generated.models.Error
import io.github.samolego.ascendo_trainboard.api.generated.models.Grade
import io.github.samolego.ascendo_trainboard.api.generated.models.LoginRequest
import io.github.samolego.ascendo_trainboard.api.generated.models.LoginResponse
import io.github.samolego.ascendo_trainboard.api.generated.models.Problem
import io.github.samolego.ascendo_trainboard.api.generated.models.ProblemGrades
import io.github.samolego.ascendo_trainboard.api.generated.models.ProblemList
import io.github.samolego.ascendo_trainboard.api.generated.models.RegisterRequest
import io.github.samolego.ascendo_trainboard.api.generated.models.Sector
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.api.generated.models.SubmitGradeRequest
import io.github.samolego.ascendo_trainboard.api.generated.models.UpdateProblemRequest
import io.github.samolego.ascendo_trainboard.getPlatform
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Simple API client for AscendoTrainBoard backend.
 * Uses generated models from OpenAPI spec.
 */
class AscendoApi(
    private val baseUrl: String = "http://192.168.1.1/api/v1"
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })

        }

        install(HttpTimeout) {
            requestTimeoutMillis = 2_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
        }
    }

    private var authToken: String? = null
    var username: String? = null
        private set

    /**
     * Helper function to handle API responses with proper error parsing
     */
    private suspend inline fun <reified T> safeApiCall(
        crossinline block: suspend () -> HttpResponse
    ): Result<T> {
        return try {
            val response = block()
            if (response.status.isSuccess()) {
                Result.success(response.body<T>())
            } else {
                // Try to parse as Error response
                val error = try {
                    response.body<Error>()
                } catch (_: Exception) {
                    // Fallback if error parsing fails
                    Error(
                        error = "HTTP ${response.status.value}: ${response.status.description}",
                        code = "HTTP_ERROR"
                    )
                }
                Result.failure(ApiException(error, response.status.value))
            }
        } catch (e: Exception) {
            // Network or other errors
            Result.failure(e)
        }
    }

    // Auth endpoints
    suspend fun register(username: String, password: String): Result<LoginResponse> {
        return safeApiCall {
            client.post("$baseUrl/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(username = username, password = password))
            }
        }
    }

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return safeApiCall<LoginResponse> {
            client.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username = username, password = password))
            }
        }.onSuccess { response ->
            authToken = response.token
            this@AscendoApi.username = response.username
            getPlatform().storage.saveLoginInfo(response)
        }
    }

    suspend fun logout(): Result<Unit> {
        return safeApiCall<Unit> {
            client.post("$baseUrl/auth/logout") {
                authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }.onSuccess {
            authToken = null
            username = null
        }
    }

    suspend fun restartSession(data: LoginResponse): Result<LoginResponse> {
        return safeApiCall<LoginResponse> {
            client.get("$baseUrl/auth/rotate_token") {
                header(HttpHeaders.Authorization, "Bearer ${data.token}")
            }
        }.onSuccess { response ->
            authToken = response.token
            username = response.username
            getPlatform().storage.saveLoginInfo(response)
        }.onFailure {
            username = null
        }
    }

    suspend fun getSectors(): Result<List<SectorSummary>> {
        return safeApiCall {
            client.get("$baseUrl/sectors")
        }
    }

    suspend fun getSector(id: Int): Result<Sector> {
        return safeApiCall {
            client.get("$baseUrl/sectors/$id")
        }
    }

    fun getSectorImageUrl(id: Int): String = "$baseUrl/sectors/$id/image"

    // Problem endpoints
    suspend fun getProblems(
        sector: Int? = null,
        minGrade: Int? = null,
        maxGrade: Int? = null,
        name: String? = null,
        page: Int = 1,
        perPage: Int = 20
    ): Result<ProblemList> {
        return safeApiCall {
            client.get("$baseUrl/problems") {
                sector?.let { parameter("sector_id", it) }
                minGrade?.let { parameter("min_grade", it) }
                maxGrade?.let { parameter("max_grade", it) }
                name?.let { parameter("name", it) }
                parameter("page", page)
                parameter("per_page", perPage)
            }
        }
    }

    suspend fun getProblem(id: Int): Result<Problem> {
        return safeApiCall {
            client.get("$baseUrl/problems/$id")
        }
    }

    suspend fun createProblem(request: CreateProblemRequest): Result<Problem> {
        return safeApiCall {
            client.post("$baseUrl/problems") {
                authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun updateProblem(id: Int, request: UpdateProblemRequest): Result<Problem> {
        return safeApiCall {
            client.put("$baseUrl/problems/$id") {
                authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun deleteProblem(id: Int): Result<Unit> {
        return safeApiCall {
            client.delete("$baseUrl/problems/$id") {
                authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }
    }

    // Grade endpoints
    suspend fun getProblemGrades(id: Int): Result<ProblemGrades> {
        return safeApiCall {
            client.get("$baseUrl/problems/$id/grades")
        }
    }

    suspend fun submitGrade(problemId: Int, request: SubmitGradeRequest): Result<Grade> {
        return safeApiCall {
            client.post("$baseUrl/problems/$problemId/grades") {
                authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    // Token management
    fun setToken(token: String) {
        authToken = token
    }

    fun getToken(): String? = authToken

    fun clearToken() {
        authToken = null
    }

    fun isAuthenticated(): Boolean = authToken != null

    // Cleanup
    fun close() {
        client.close()
    }
}
