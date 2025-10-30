package io.github.samolego.ascendo_trainboard.api

import io.github.samolego.ascendo_trainboard.api.generated.models.CreateProblemRequest
import io.github.samolego.ascendo_trainboard.api.generated.models.Grade
import io.github.samolego.ascendo_trainboard.api.generated.models.LoginRequest
import io.github.samolego.ascendo_trainboard.api.generated.models.Problem
import io.github.samolego.ascendo_trainboard.api.generated.models.ProblemGrades
import io.github.samolego.ascendo_trainboard.api.generated.models.ProblemList
import io.github.samolego.ascendo_trainboard.api.generated.models.RegisterRequest
import io.github.samolego.ascendo_trainboard.api.generated.models.Sector
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.api.generated.models.SubmitGradeRequest
import io.github.samolego.ascendo_trainboard.api.generated.models.UpdateProblemRequest
import io.github.samolego.ascendo_trainboard.api.generated.models.User
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
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Simple API client for AscendoTrainBoard backend.
 * Uses generated models from OpenAPI spec.
 */
class AscendoApi(
    private val baseUrl: String = "http://localhost:3000/api/v1"
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

    // Auth endpoints
    suspend fun register(username: String, password: String): Result<User> {
        return try {
            val response: User = client.post("$baseUrl/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(username = username, password = password))
            }.body()

            // Note: Backend returns token in different response, adjust if needed
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<Pair<String, String>> {
        return try {
            val response = client.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username = username, password = password))
            }

            // Extract token from response body (adjust based on actual response)
            val body: String = response.body()
            // Parse token and username from response
            // This is a simplified version - adjust based on actual backend response
            authToken = "extracted_token" // TODO: Parse from response
            Result.success(Pair("token", username))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            client.post("$baseUrl/auth/logout") {
                authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
            authToken = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sector endpoints
    suspend fun getSectors(): Result<List<SectorSummary>> {
        return try {
            val response: List<SectorSummary> = client.get("$baseUrl/sectors").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSector(id: Int): Result<Sector> {
        return try {
            val response: Sector = client.get("$baseUrl/sectors/$id").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSectorImageUrl(id: Int): String = "$baseUrl/sectors/$id/image"

    // Problem endpoints
    suspend fun getProblems(
        sector: String? = null,
        minGrade: Int? = null,
        maxGrade: Int? = null,
        name: String? = null,
        page: Int = 1,
        perPage: Int = 20
    ): Result<ProblemList> {
        return try {
            val response: ProblemList = client.get("$baseUrl/problems") {
                sector?.let { parameter("sector", it) }
                minGrade?.let { parameter("min_grade", it) }
                maxGrade?.let { parameter("max_grade", it) }
                name?.let { parameter("name", it) }
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProblem(id: Int): Result<Problem> {
        return try {
            val response: Problem = client.get("$baseUrl/problems/$id").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProblem(request: CreateProblemRequest): Result<Problem> {
        return try {
            val response: Problem = client.post("$baseUrl/problems") {
                authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProblem(id: Int, request: UpdateProblemRequest): Result<Problem> {
        return try {
            val response: Problem = client.put("$baseUrl/problems/$id") {
                authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProblem(id: Int): Result<Unit> {
        return try {
            client.delete("$baseUrl/problems/$id") {
                authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Grade endpoints
    suspend fun getProblemGrades(id: Int): Result<ProblemGrades> {
        return try {
            val response: ProblemGrades = client.get("$baseUrl/problems/$id/grades").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitGrade(problemId: Int, request: SubmitGradeRequest): Result<Grade> {
        return try {
            val response: Grade = client.post("$baseUrl/problems/$problemId/grades") {
                authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
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
