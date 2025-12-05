package io.github.samolego.ascendo_trainboard.ui.problems.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.api.HoldType
import io.github.samolego.ascendo_trainboard.api.ProblemHold
import io.github.samolego.ascendo_trainboard.api.generated.models.Grade
import io.github.samolego.ascendo_trainboard.api.generated.models.Problem
import io.github.samolego.ascendo_trainboard.api.generated.models.Sector
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.ui.components.AvgStarsBadge
import io.github.samolego.ascendo_trainboard.ui.components.EmptyState
import io.github.samolego.ascendo_trainboard.ui.components.GradeBadge
import io.github.samolego.ascendo_trainboard.ui.components.GradeSelector
import io.github.samolego.ascendo_trainboard.ui.components.UserRatingCard
import io.github.samolego.ascendo_trainboard.ui.components.ZoomableSectorProblemImage
import io.github.samolego.ascendo_trainboard.ui.components.dialog.ProblemDeleteDialog
import io.github.samolego.ascendo_trainboard.ui.components.dialog.ProblemRatingDialog
import io.github.samolego.ascendo_trainboard.ui.components.error.ErrorBottomBar
import kotlin.math.roundToInt
import kotlin.time.Clock.System.now
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemDetailsScreen(
    viewModel: ProblemDetailsViewModel,
    onNavigateBack: () -> Unit,
    availableSectors: List<SectorSummary>? = null,
) {
    val state by viewModel.state.collectAsState()
    val editMode = state.inEditMode && state.canEdit
    var showSectorDialog by remember { mutableStateOf(state.inCreateMode && availableSectors != null) }
    var showGradeDialog by remember { mutableStateOf(false) }
    var showEditMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isWideScreen by remember { mutableStateOf(false) }

    val goBack = {
        viewModel.toggleEditMode(false)
        onNavigateBack()
    }
    val shouldShowGradesInfo = !editMode && state.problem?.grades?.isNotEmpty() == true && state.problem?.averageGrade != null && state.problem?.averageStars != null

    BottomSheetScaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = goBack,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    if (editMode) {
                        TextField(
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            ),
                            label = { Text("Ime smeri") },
                            value = state.problem?.name ?: "",
                            onValueChange = {
                                viewModel.setProblemName(it)
                            }
                        )
                    } else {
                        Text(state.problem?.name ?: "Nalaganje smeri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (state.canEdit && state.problem != null) {
                        if (editMode) {
                            IconButton(
                                onClick = {
                                    viewModel.saveCurrentProblem()
                                    viewModel.toggleEditMode()
                                }
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Edit/Save")
                            }
                        } else {
                            IconButton(
                                onClick = { showEditMenu = !showEditMenu }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Menu"
                                )
                            }

                            if (showEditMenu) {
                                DropdownMenu(
                                    expanded = showEditMenu,
                                    onDismissRequest = { showEditMenu = false }
                                ) {
                                    val text = if (editMode) "Shrani" else "Uredi"
                                    DropdownMenuItem(
                                        text = { Text(text) },
                                        onClick = {
                                            if (editMode) {
                                                viewModel.saveCurrentProblem()
                                            }
                                            viewModel.toggleEditMode()
                                            showEditMenu = false
                                        },
                                        leadingIcon = {
                                            val iconVec =
                                                if (editMode) Icons.Default.Save else Icons.Default.Edit
                                            Icon(
                                                imageVector = iconVec,
                                                contentDescription = null
                                            )
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Izbriši") },
                                        onClick = {
                                            showDeleteDialog = true
                                            showEditMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        sheetPeekHeight = if (shouldShowGradesInfo && !isWideScreen) 96.dp else 0.dp,
        sheetSwipeEnabled = shouldShowGradesInfo && !isWideScreen,
        sheetContent = {
            if (shouldShowGradesInfo && !isWideScreen) {
                GradesInfo(
                    grades = state.problem?.grades ?: emptyList(),
                    averageStars = state.problem?.averageStars ?: 0f,
                    averageGrade = state.problem?.averageGrade ?: 0f,
                )
            }
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            isWideScreen = maxWidth > maxHeight && maxWidth > 600.dp

            val outerScrollModifier =
                if (isWideScreen) Modifier else Modifier.verticalScroll(rememberScrollState())

            Column(modifier = outerScrollModifier.fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)
                    )
                } else if (state.sector != null) {
                    if (state.problem == null) {
                        EmptyState(
                            titleMessage = "Ta smer ni bila najdena",
                            subtitleMessage = "Poskusi poiskati drugo ..."
                        )
                    } else {
                        val leftContent = @Composable {
                            Spacer(modifier = Modifier.height(16.dp))
                            ProblemDetails(
                                problem = state.problem!!,
                                sector = state.sector!!,
                                imageUrl = viewModel.getSectorImageUrl(state.sector!!.id),
                                editable = editMode,
                                holds = if (editMode) {
                                    state.editableHolds.values.toList()
                                } else {
                                    state.problem!!.holdSequence.mapNotNull {
                                        ProblemHold.fromList(
                                            it
                                        )
                                    }
                                },
                                onHoldUpdated = viewModel::updateHold,
                                onHoldRemoved = viewModel::removeHold,
                                getHoldByIndex = viewModel::getHoldByIndex
                            )
                        }

                        val rightContent = @Composable {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                if (editMode) {
                                    var grade by remember {
                                        mutableStateOf(
                                            state.problem?.grade ?: 0
                                        )
                                    }
                                    // Choose how hard the problem is
                                    GradeSelector(
                                        grade = grade,
                                        onGradeChanged = {
                                            viewModel.setProblemGrade(it)
                                            grade = it
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Opis smeri") },
                                        value = state.problem?.description ?: "",
                                        onValueChange = {
                                            viewModel.setProblemDescription(it)
                                        }
                                    )
                                } else {
                                    val content = @Composable {
                                        Column {
                                            GradeBadge(
                                                grade = state.problem?.grade ?: 0,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                            state.problem?.description?.let {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = it,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (isWideScreen) {
                                        GradesInfo(
                                            grades = state.problem?.grades ?: emptyList(),
                                            averageStars = state.problem?.averageStars ?: 0f,
                                            averageGrade = state.problem?.averageGrade ?: 0f,
                                        ) {
                                            content()
                                        }
                                    } else {
                                        content()
                                    }
                                }
                            }

                        }

                        if (isWideScreen) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(
                                    Modifier
                                        .weight(2f)
                                        .padding(end = 8.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    leftContent()
                                }
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                ) {
                                    rightContent()
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                            ) {
                                leftContent()
                                rightContent()
                            }
                        }
                    }
                }
            }


            if (!editMode) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    FloatingActionButton(
                        modifier = Modifier.shadow(4.dp),
                        onClick = {
                            showGradeDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Mark as climbed")
                    }
                }
            }
        }
    }

    state.error?.let { err ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            ErrorBottomBar(
                error = err,
                onDismiss = {
                    viewModel.clearError()
                },
            )
        }
    }

    if (showGradeDialog && state.problem != null) {
        ProblemRatingDialog(
            defaultGrade = state.problem!!.grade,
            defaultRating = 5,
            onConfirm = {
                viewModel.postGrade(it)
                showGradeDialog = false
            },
            onDismiss = {
                showGradeDialog = false
            }
        )
    }

    if (showSectorDialog) {
        SectorChooserDialog(
            sectors = availableSectors!!,
            onChoose = {
                it?.let {
                    viewModel.setCreateModeSector(it)
                    showSectorDialog = false
                }
            },
            onDismiss = goBack,
        )
    }

    if (showDeleteDialog) {
        ProblemDeleteDialog(
            name = state.problem?.name ?: "",
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteProblem(
                    onSuccess = goBack
                )
            },
            onDismiss = {
                showDeleteDialog = false
            },
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun ProblemDetails(
    modifier: Modifier = Modifier,
    problem: Problem,
    sector: Sector,
    imageUrl: String,
    editable: Boolean,
    holds: List<ProblemHold>,
    onHoldUpdated: (Int, ProblemHold) -> Unit,
    onHoldRemoved: (Int) -> Unit = {},
    getHoldByIndex: (Int) -> ProblemHold? = { null },
) {
    var selectedHold by remember { mutableStateOf<ProblemHold?>(null) }
    var lastHoldClickTime by remember {
        mutableStateOf(Instant.fromEpochSeconds(0))
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ZoomableSectorProblemImage(
            sectorImageUrl = imageUrl,
            sector = sector,
            holds = holds,
            onHoldClicked = { holdIndex ->
                val time = now()
                val difference = time - lastHoldClickTime
                lastHoldClickTime = time

                if (difference.inWholeMilliseconds < 80) {
                    // Mobile browser recognizes 2 clicks for some reason
                    // so we must limit it
                    return@ZoomableSectorProblemImage
                }

                if (selectedHold?.holdIndex == holdIndex) {
                    onHoldRemoved(holdIndex)
                    selectedHold = null
                } else {
                    val type = if (holds.isEmpty()) HoldType.START else HoldType.NORMAL
                    val newHold =
                        getHoldByIndex(holdIndex) ?: ProblemHold(holdIndex, type)
                    selectedHold = newHold
                    onHoldUpdated(holdIndex, newHold)
                }
            },
            selectedHold = selectedHold,
            interactive = editable,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = problem.author,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (selectedHold != null && editable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Opriimek #${selectedHold!!.holdIndex}")
                IconButton(
                    onClick = {
                        onHoldRemoved(selectedHold!!.holdIndex)
                        selectedHold = null
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                HoldType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = HoldType.entries.size,
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = type.outlineColor,
                            activeContentColor = Color.Black,
                        ),
                        onClick = {
                            val updatedHold = ProblemHold(selectedHold!!.holdIndex, type)
                            onHoldUpdated(selectedHold!!.holdIndex, updatedHold)
                            selectedHold = updatedHold
                        },
                        selected = (type == selectedHold!!.holdType),
                    ) {
                        Text(
                            text = type.getTypeName(),
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun GradesInfo(
    grades: List<Grade>,
    averageStars: Float,
    averageGrade: Float,
    preContent: @Composable () -> Unit = {}
) {

    LazyVerticalGrid(
        modifier = Modifier.padding(horizontal = 16.dp),
        columns = GridCells.Adaptive(minSize = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            preContent()
        }

        if (grades.isNotEmpty()) {
            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                    ) {
                        Text(
                            "Povprečje",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterStart),
                        )

                        AvgStarsBadge(
                            modifier = Modifier.align(Alignment.Center),
                            stars = averageStars,
                        )

                        GradeBadge(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            grade = averageGrade.roundToInt(),
                            usePrefixText = false,
                        )
                    }

                }
            }
            items(grades) { grade ->
                UserRatingCard(
                    modifier = Modifier
                        .fillMaxWidth(),
                    grade = grade,
                )
            }
        }
    }
}
