package io.github.samolego.ascendo_trainboard.ui.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("auth")
data object Authenticate: Route

@Serializable
@SerialName("problems")
data object Problems: Route

@Serializable
@SerialName("problems/create")
data object CreateProblem : Route

@Serializable
@SerialName("problems/details")
data class ProblemDetails(val problemId: Int): Route

@Serializable
@SerialName("problems/edit")
data class EditProblem(val problemId: Int): Route

interface Route
