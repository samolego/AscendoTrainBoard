package io.github.samolego.ascendo_trainboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import io.github.samolego.ascendo_trainboard.api.AscendoApi
import io.github.samolego.ascendo_trainboard.ui.problems.ProblemListScreen
import io.github.samolego.ascendo_trainboard.ui.problems.ProblemListViewModel

@Composable
fun App() {
    MaterialTheme {
        val api = remember { AscendoApi(baseUrl = "http://localhost:3000/api/v1") }
        val viewModel = remember { ProblemListViewModel(api) }

        ProblemListScreen(
            viewModel = viewModel,
            onProblemClick = { problemId ->
                println("Clicked problem: $problemId")
                // TODO: Navigate to problem detail screen
            }
        )
    }
}
