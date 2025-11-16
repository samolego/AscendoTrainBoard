package io.github.samolego.ascendo_trainboard.ui.problems.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.samolego.ascendo_trainboard.api.AscendoApi
import io.github.samolego.ascendo_trainboard.api.ProblemHold
import io.github.samolego.ascendo_trainboard.api.generated.models.CreateProblemRequest
import io.github.samolego.ascendo_trainboard.api.generated.models.Problem
import io.github.samolego.ascendo_trainboard.api.generated.models.Sector
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.api.generated.models.UpdateProblemRequest
import io.github.samolego.ascendo_trainboard.ui.components.error.ErrorUiState
import io.github.samolego.ascendo_trainboard.ui.components.error.toErrorUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProblemDetailsState(
    val problem: Problem? = null,
    val sector: Sector? = null,
    val isLoading: Boolean = false,
    val error: ErrorUiState? = null,
    val inEditMode: Boolean = false,
    val canEdit: Boolean = false,
    val editableHolds: Map<Int, ProblemHold> = emptyMap(),
) {
    val inCreateMode = problem?.id == -1 && inEditMode
}

class ProblemDetailsViewModel(
    private val api: AscendoApi
) : ViewModel() {

    private val _state = MutableStateFlow(ProblemDetailsState())
    val state: StateFlow<ProblemDetailsState> = _state.asStateFlow()


    fun loadProblem(refresh: Boolean = false, problemId: Int) {
        if (problemId < 0) {
            return
        }

        viewModelScope.launch {
            if (refresh) {
                _state.update { it.copy(isLoading = true, problem = null) }
            } else {
                _state.update { it.copy(isLoading = true) }
            }
            val problemResult = api.getProblem(problemId)

            problemResult.onSuccess { problem ->
                val sectorResult = api.getSector(problem.sectorId)

                sectorResult.onSuccess { sector ->
                    _state.update {
                        it.copy(
                            problem = problem,
                            sector = sector,
                            isLoading = false,
                            inEditMode = false,
                            canEdit = api.username == problem.author,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.toErrorUiState(),
                            canEdit = false,
                            inEditMode = false,
                        )
                    }
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.toErrorUiState(),
                        canEdit = false,
                        inEditMode = false,
                    )
                }
            }
        }
    }

    fun setProblem(problemId: Int) {
        loadProblem(problemId = problemId, refresh = true)
    }

    fun getSectorImageUrl(sectorId: Int): String =
        api.getSectorImageUrl(sectorId)

    fun toggleEditMode(forceStatus: Boolean? = null) {
        val newEditMode = forceStatus ?: !state.value.inEditMode
        _state.update {
            it.copy(
                inEditMode = newEditMode,
                editableHolds = if (newEditMode) {
                    it.problem?.holdSequence
                        ?.mapNotNull { hold -> ProblemHold.fromList(hold) }
                        ?.associateBy { hold -> hold.holdIndex }
                        ?: emptyMap()
                } else {
                    emptyMap()
                }
            )
        }
    }

    fun updateHold(holdIndex: Int, hold: ProblemHold) {
        if (state.value.inEditMode) {
            _state.update {
                it.copy(editableHolds = it.editableHolds + (holdIndex to hold))
            }
        }
    }

    fun removeHold(holdIndex: Int) {
        if (state.value.inEditMode) {
            _state.update {
                it.copy(editableHolds = it.editableHolds - holdIndex)
            }
        }
    }

    fun getHoldByIndex(index: Int): ProblemHold? {
        return state.value.editableHolds[index]
    }


    fun saveCurrentProblem() {
        val problem = state.value.problem ?: return
        if (!state.value.inEditMode) {
            return
        }

        viewModelScope.launch {
            val updatedHoldSequence = state.value.editableHolds.values
                .sortedBy { it.holdIndex }
                .map { listOf(it.holdIndex, it.holdType.ordinal) }

            val updatedProblem = problem.copy(
                name = problem.name,
                description = problem.description,
                grade = problem.grade,
                holdSequence = updatedHoldSequence,
            )

            val status = if (state.value.inCreateMode) {
                val request = CreateProblemRequest(
                    name = updatedProblem.name,
                    description = updatedProblem.description,
                    grade = updatedProblem.grade,
                    holdSequence = updatedProblem.holdSequence,
                    sectorId = updatedProblem.sectorId,
                )

                api.createProblem(request)
            } else {
                val request = UpdateProblemRequest(
                    name = updatedProblem.name,
                    description = updatedProblem.description,
                    grade = updatedProblem.grade,
                    holdSequence = updatedProblem.holdSequence,
                )

                api.updateProblem(updatedProblem.id, request)
            }

            status.onSuccess {
                _state.update {
                    it.copy(problem = updatedProblem)
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        error = error.toErrorUiState(),
                    )
                }
            }
        }
    }

    fun enterCreateMode(): Boolean {
        if (!api.isAuthenticated()) {
            return false
        }

        val problem = Problem(
            id = -1,
            name = "",
            author = api.username ?: "",
            grade = 8,
            sectorId = -1,
            holdSequence = listOf(),
        )
        _state.update {
            it.copy(
                sector = null,
                canEdit = true,
                inEditMode = true,
                problem = problem,
                editableHolds = problem.holdSequence
                    .mapNotNull { hold -> ProblemHold.fromList(hold) }
                    .associateBy { hold -> hold.holdIndex }
            )
        }

        return true
    }

    fun setCreateModeSector(sector: SectorSummary) {
        if (state.value.inCreateMode) {
            viewModelScope.launch {
                api.getSector(sector.id)
                    .onSuccess { sector ->
                        _state.update {
                            it.copy(
                                problem = it.problem?.copy(
                                    sectorId = sector.id,
                                ),
                                sector = sector,
                            )
                        }
                    }
                    .onFailure {error ->
                        _state.update {
                            it.copy(
                                error = error.toErrorUiState(),
                            )
                        }
                    }
            }
        }
    }

    fun setProblemGrade(grade: Int) {
        if ( state.value.problem == null || grade == state.value.problem?.grade) {
            return
        }

        _state.update {
            it.copy(problem = it.problem!!.copy(grade = grade))
        }
    }

    fun setProblemDescription(description: String) {
        if (state.value.problem != null) {
            _state.update {
                it.copy(problem = it.problem!!.copy(description = description))
            }
        }
    }

    fun setProblemName(name: String) {
        if (state.value.problem != null) {
            _state.update {
                it.copy(problem = it.problem!!.copy(name = name))
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun deleteProblem(onSuccess: () -> Unit) {
        if (state.value.problem == null || !state.value.canEdit) {
            return
        }

        viewModelScope.launch {
            api.deleteProblem(state.value.problem!!.id)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            error = err.toErrorUiState(),
                        )
                    }
                }
        }
    }
}
