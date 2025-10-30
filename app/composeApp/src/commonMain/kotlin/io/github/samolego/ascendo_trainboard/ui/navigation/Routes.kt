package io.github.samolego.ascendo_trainboard.ui.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("auth/login")
data object Login

@Serializable
@SerialName("auth/register")
data object Register


@Serializable
@SerialName("problems")
data object Problems

@Serializable
@SerialName("problems/create")
data object CreateProblem

@Serializable
@SerialName("problems/details")
data class ProblemDetails(val problemId: Int)

@Serializable
@SerialName("problems/edit")
data class EditProblem(val problemId: Int)
