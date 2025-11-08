package io.github.samolego.ascendo_trainboard.ui.problems.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.ui.components.EmptyState
import io.github.samolego.ascendo_trainboard.ui.components.error.ErrorBottomBar
import io.github.samolego.ascendo_trainboard.ui.navigation.Authenticate
import io.github.samolego.ascendo_trainboard.ui.navigation.CreateProblem
import io.github.samolego.ascendo_trainboard.ui.navigation.ProblemDetails
import io.github.samolego.ascendo_trainboard.ui.navigation.Route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemListScreen(
    viewModel: ProblemListViewModel,
    modifier: Modifier = Modifier,
    onNavigateTo: (Route) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    val sectorId2name by remember(state.sectors) {
        lazy { state.sectors.associateBy { it.id }.mapValues { it.value.name } }
    }


    // Detect when user scrolls near bottom
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= state.problems.size - 5 &&
                    !state.isLoadingMore &&
                    state.hasMore
                ) {
                    viewModel.loadMore()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ascendo TrainBoard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { onNavigateTo(Authenticate) }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                },
            )
        },
        bottomBar = {
            ErrorBottomBar(
                error = state.error,
                onDismiss = viewModel::clearError
            )
        },
        modifier = modifier,
        floatingActionButton = {
            if (viewModel.isAuthenticated()) {
                FloatingActionButton(
                    onClick = { onNavigateTo(CreateProblem) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Bar
            FilterBar(
                sectors = state.sectors,
                selectedSector = state.selectedSector,
                minGrade = state.minGrade,
                maxGrade = state.maxGrade,
                searchAuthor = state.searchAuthor,
                onSectorSelected = viewModel::setSectorFilter,
                onGradeRangeChanged = viewModel::setGradeRange,
                onAuthorChanged = viewModel::setAuthorSearch,
                onClearFilters = viewModel::clearFilters,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            // Problem List
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = state.isLoading && state.problems.isNotEmpty(),
                onRefresh = viewModel::refresh,
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.isLoading && state.problems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillParentMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (!state.isLoading && state.problems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillParentMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyState(
                                    titleMessage = "Ni najdenih smeri",
                                    subtitleMessage = "Poskusi olajšati filtre ..."
                                )
                            }
                        }
                    } else {
                        items(state.problems) { problem ->
                            ProblemCard(
                                problem = problem,
                                sectorName = sectorId2name[problem.sectorId] ?: "Neznan sektor",
                                onClick = { onNavigateTo(ProblemDetails(problem.id)) }
                            )
                        }

                        // Loading more indicator
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
