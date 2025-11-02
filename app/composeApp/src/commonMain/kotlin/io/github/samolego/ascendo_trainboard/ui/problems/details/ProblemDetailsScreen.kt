package io.github.samolego.ascendo_trainboard.ui.problems.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.api.HoldType
import io.github.samolego.ascendo_trainboard.api.ProblemHold
import io.github.samolego.ascendo_trainboard.api.generated.models.Problem
import io.github.samolego.ascendo_trainboard.api.generated.models.Sector
import io.github.samolego.ascendo_trainboard.ui.components.EmptyState
import io.github.samolego.ascendo_trainboard.ui.components.ZoomableSectorProblemImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemDetailsScreen(
    viewModel: ProblemDetailsViewModel,
    onNavigateBack: () -> Unit,
    problemId: Int,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(problemId) {
        viewModel.setProblem(problemId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(state.problem?.name ?: "Smer #${state.problemId}") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (state.canEdit && state.problem != null) {
                        IconButton(
                            onClick = {
                                if (state.inEditMode) {
                                    viewModel.updateActiveProblem()
                                }
                                viewModel.toggleEditMode()
                            }
                        ) {
                            val iconVec =
                                if (state.inEditMode) Icons.Default.Save else Icons.Default.Edit
                            Icon(iconVec, contentDescription = "Edit/Save")
                        }
                    }
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    if (state.sector == null) {
                        EmptyState(
                            titleMessage = "Sektor te smeri ne obstaja",
                            subtitleMessage = "Poskusi izbrati drugo smer ..."
                        )
                    } else if (state.problem == null) {
                        EmptyState(
                            titleMessage = "Ta smer ni bila najdena",
                            subtitleMessage = "Poskusi poiskati drugo ..."
                        )
                    } else {
                        ProblemDetails(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            problem = state.problem!!,
                            sector = state.sector!!,
                            imageUrl = viewModel.getSectorImageUrl(state.sector!!.name),
                            editable = state.inEditMode,
                            holds = if (state.inEditMode) {
                                state.editableHolds.values.toList()
                            } else {
                                state.problem!!.holdSequence.mapNotNull { ProblemHold.fromList(it) }
                            },
                            onHoldUpdated = viewModel::updateHold,
                            onHoldRemoved = viewModel::removeHold,
                            getHoldByIndex = viewModel::getHoldByIndex
                        )
                    }
                }
            }
        }
    }
}

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

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ZoomableSectorProblemImage(
            sectorImageUrl = imageUrl,
            sector = sector,
            holds = holds,
            onHoldClicked = { holdIndex ->
                if (selectedHold?.holdIndex == holdIndex) {
                    onHoldRemoved(holdIndex)
                    selectedHold = null
                } else {
                    selectedHold = getHoldByIndex(holdIndex) ?: ProblemHold(holdIndex, HoldType.NORMAL)
                    onHoldUpdated(holdIndex, ProblemHold(holdIndex, selectedHold!!.holdType))
                }
            },
            selectedHold = selectedHold,
            interactive = editable,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HoldType.entries.forEach {
                    val text = it.getTypeName()
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = (it == selectedHold!!.holdType),
                                onClick = {
                                    val updatedHold = ProblemHold(selectedHold!!.holdIndex, it)
                                    onHoldUpdated(selectedHold!!.holdIndex, updatedHold)
                                    selectedHold = updatedHold
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (it == selectedHold!!.holdType),
                            onClick = null
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
