package io.github.samolego.ascendo_trainboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import io.github.samolego.ascendo_trainboard.api.AscendoApi
import io.github.samolego.ascendo_trainboard.ui.authentication.AuthenticationViewModel
import io.github.samolego.ascendo_trainboard.ui.navigation.AppNavigation
import io.github.samolego.ascendo_trainboard.ui.problems.details.ProblemDetailsViewModel
import io.github.samolego.ascendo_trainboard.ui.problems.list.ProblemListViewModel

@Composable
fun App(
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    MaterialTheme {
        val platform = getPlatform()
        val api = remember { AscendoApi(baseUrl = "${platform.baseUrl(true)}/api/v1") }
        val problemListViewModel = remember { ProblemListViewModel(api) }
        val problemDetailsViewModel = remember { ProblemDetailsViewModel(api) }
        val authViewModel = remember { AuthenticationViewModel(api) }

        val navController = rememberNavController()

        LaunchedEffect(Unit) {
            authViewModel.restoreSession(platform.storage::loadLoginInfo)
        }

        AppNavigation(
            navController = navController,
            onNavHostReady = onNavHostReady,
            problemListViewModel = problemListViewModel,
            problemDetailsViewModel = problemDetailsViewModel,
            authViewModel = authViewModel,
        )
    }
}
