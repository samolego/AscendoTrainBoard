package io.github.samolego.ascendo_trainboard.ui.problems.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.samolego.ascendo_trainboard.api.AscendoApi
import io.github.samolego.ascendo_trainboard.api.ProblemHold
import io.github.samolego.ascendo_trainboard.api.generated.models.Problem
import io.github.samolego.ascendo_trainboard.api.generated.models.Sector
import io.github.samolego.ascendo_trainboard.api.generated.models.UpdateProblemRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProblemDetailsState(
    val problemId: Int = -1,
    val problem: Problem? = null,
    val sector: Sector? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val inEditMode: Boolean = false,
    val canEdit: Boolean = false,
    val editableHolds: Map<Int, ProblemHold> = emptyMap(),
)

class ProblemDetailsViewModel(
    private val api: AscendoApi
) : ViewModel() {

    private val _state = MutableStateFlow(ProblemDetailsState())
    val state: StateFlow<ProblemDetailsState> = _state.asStateFlow()

    init {
        loadProblem()
    }

    fun loadProblem(refresh: Boolean = false) {
        if (state.value.problemId < 0) {
            return
        }

        println("Loading problems, refresh=$refresh")
        viewModelScope.launch {
            if (refresh) {
                _state.update { it.copy(isLoading = true, problem = null) }
            } else {
                _state.update { it.copy(isLoading = true) }
            }

            println("Current state before loading: ${_state.value}")
            val problemResult = api.getProblem(_state.value.problemId)

            println("Load problems result: $problemResult")

            problemResult.onSuccess { problem ->
                val sectorResult = api.getSector(problem.sectorName)

                sectorResult.onSuccess { sector ->
                    _state.update {
                        it.copy(
                            problem = problem,
                            sector = sector,
                            isLoading = false,
                            canEdit = api.username == problem.author,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Napaka pri nalaganju sektorja",
                            canEdit = false,
                        )
                    }
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Napaka pri nalaganju smeri",
                        canEdit = false,
                    )
                }
            }
        }
    }

    fun setProblem(problemId: Int) {
        if (problemId != state.value.problemId) {
            _state.update { it.copy(problemId = problemId) }
            loadProblem(refresh = true)
        }
    }

    fun getSectorImageUrl(sectorName: String): String =
        api.getSectorImageUrl(sectorName)

    fun toggleEditMode() {
        val newEditMode = !state.value.inEditMode
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
            println("Holds before: ${state.value.editableHolds}")
            println("Updating hold $holdIndex to $hold")
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


    fun updateActiveProblem() {
        val problem = state.value.problem ?: return

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

            val request = UpdateProblemRequest(
                name = updatedProblem.name,
                description = updatedProblem.description,
                grade = updatedProblem.grade,
                holdSequence = updatedProblem.holdSequence,
            )

            api.updateProblem(updatedProblem.id, request)
            _state.update {
                it.copy(problem = updatedProblem)
            }
        }
    }
}
