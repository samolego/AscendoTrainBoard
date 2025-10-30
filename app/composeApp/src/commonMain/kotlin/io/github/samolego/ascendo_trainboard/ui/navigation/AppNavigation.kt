package io.github.samolego.ascendo_trainboard.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import io.github.samolego.ascendo_trainboard.getPlatform
import io.github.samolego.ascendo_trainboard.ui.problems.create.CreateProblemScreen
import io.github.samolego.ascendo_trainboard.ui.problems.create.CreateProblemViewModel
import io.github.samolego.ascendo_trainboard.ui.problems.details.ProblemDetailsScreen
import io.github.samolego.ascendo_trainboard.ui.problems.details.ProblemDetailsViewModel
import io.github.samolego.ascendo_trainboard.ui.problems.list.ProblemListScreen
import io.github.samolego.ascendo_trainboard.ui.problems.list.ProblemListViewModel

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    onNavHostReady: suspend (NavController) -> Unit = {},
    problemListViewModel: ProblemListViewModel,
    createProblemViewModel: CreateProblemViewModel,
    problemDetailsViewModel: ProblemDetailsViewModel
) {
    val baseUrl = getPlatform().baseUrl(false)

    NavHost(
        navController = navController,
        startDestination = Problems,
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) }
    ) {
        composable<Problems>(
            deepLinks =
                listOf(navDeepLink { uriPattern = "$baseUrl/problems" }),
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) }
        ) {
            ProblemListScreen(
                viewModel = problemListViewModel,
                onProblemClick = {
                    navController.navigate(ProblemDetails(it))
                }
            )
        }

        composable<ProblemDetails>(
            deepLinks = listOf(navDeepLink { uriPattern = "$baseUrl/problems/{id}" }),
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) },
        ) {
            ProblemDetailsScreen(
                viewModel = problemDetailsViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }


        composable<CreateProblem>(
            deepLinks = listOf(navDeepLink { uriPattern = "$baseUrl/problems/create" }),
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) }
        ) {
            CreateProblemScreen(
                viewModel = createProblemViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }

    LaunchedEffect(navController) { onNavHostReady(navController) }
}
