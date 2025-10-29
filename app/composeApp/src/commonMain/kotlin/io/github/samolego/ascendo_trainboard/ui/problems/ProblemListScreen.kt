package io.github.samolego.ascendo_trainboard.ui.problems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.api.generated.models.ProblemSummary
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.ui.getFrenchGrade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemListScreen(
    viewModel: ProblemListViewModel,
    onProblemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

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
                title = { Text("Climbing Problems") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
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
                onAuthorSearch = viewModel::applyAuthorFilter,
                onClearFilters = viewModel::clearFilters,
                modifier = Modifier.fillMaxWidth()
            )

            // Error Banner
            if (state.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::clearError) {
                            Icon(Icons.Default.Clear, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            // Problem List
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading && state.problems.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (!state.isLoading && state.problems.isEmpty()) {
                    EmptyState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.problems) { problem ->
                            ProblemCard(
                                problem = problem,
                                onClick = { onProblemClick(problem.id) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    sectors: List<SectorSummary>,
    selectedSector: String?,
    minGrade: Int,
    maxGrade: Int,
    searchAuthor: String,
    onSectorSelected: (String?) -> Unit,
    onGradeRangeChanged: (Int, Int) -> Unit,
    onAuthorChanged: (String) -> Unit,
    onAuthorSearch: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAuthorSearch by remember { mutableStateOf(false) }
    var expandedSector by remember { mutableStateOf(false) }
    var currentSliderRange by remember(minGrade, maxGrade) {
        mutableStateOf(minGrade.toFloat()..maxGrade.toFloat())
    }

    Column(modifier = modifier) {
        // Sector Filter Dropdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedSector,
                onExpandedChange = { expandedSector = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedSector ?: "All Sectors",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sector") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSector)
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expandedSector,
                    onDismissRequest = { expandedSector = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Sectors") },
                        onClick = {
                            onSectorSelected(null)
                            expandedSector = false
                        }
                    )
                    sectors.forEach { sector ->
                        DropdownMenuItem(
                            text = { Text("Sector ${sector.name}") },
                            onClick = {
                                onSectorSelected(sector.name)
                                expandedSector = false
                            }
                        )
                    }
                }
            }
        }

        // Grade Range Filter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Grade Range: ${getFrenchGrade(minGrade)} - ${getFrenchGrade(maxGrade)}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            RangeSlider(
                value = currentSliderRange,
                onValueChange = { range ->
                    currentSliderRange = range
                    onGradeRangeChanged(
                        currentSliderRange.start.toInt(),
                        currentSliderRange.endInclusive.toInt()
                    )
                },
                onValueChangeFinished = {
                    onGradeRangeChanged(
                        currentSliderRange.start.toInt(),
                        currentSliderRange.endInclusive.toInt()
                    )
                },
                valueRange = 0f..32f,
                steps = 32,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Author Search & Clear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showAuthorSearch) {
                OutlinedTextField(
                    value = searchAuthor,
                    onValueChange = onAuthorChanged,
                    placeholder = { Text("Author name") },
                    trailingIcon = {
                        IconButton(onClick = onAuthorSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            } else {
                TextButton(
                    onClick = { showAuthorSearch = true }
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Search by author")
                }
            }

            if (selectedSector != null || minGrade != 1 || maxGrade != 10 || searchAuthor.isNotBlank()) {
                TextButton(onClick = {
                    onClearFilters()
                    showAuthorSearch = false
                }) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }

        HorizontalDivider()
    }
}

@Composable
fun ProblemCard(
    problem: ProblemSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = problem.name ?: "Problem ${problem.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Grade badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = getFrenchGrade(problem.grade),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Author
                Column {
                    Text(
                        text = "By",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = problem.author,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Sector
                Column {
                    Text(
                        text = "Sector",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = problem.sectorName,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Rating (if available)
                problem.averageStars?.let { stars ->
                    Column {
                        Text(
                            text = "Rating",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "⭐ ${stars.toString().take(3)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Average grade (if available)
                problem.averageGrade?.let { grade ->
                    Column {
                        Text(
                            text = "Avg Grade",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "V${grade.toString().take(3)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Description (if available)
            problem.description?.let { description ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No problems found",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try adjusting your filters",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
