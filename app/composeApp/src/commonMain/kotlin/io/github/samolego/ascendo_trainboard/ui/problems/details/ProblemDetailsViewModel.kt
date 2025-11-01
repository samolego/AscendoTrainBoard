package io.github.samolego.ascendo_trainboard.ui.problems.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.samolego.ascendo_trainboard.api.AscendoApi
import io.github.samolego.ascendo_trainboard.api.generated.models.Problem
import io.github.samolego.ascendo_trainboard.api.generated.models.Sector
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
                            isLoading = false
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Napaka pri nalaganju sektorja"
                        )
                    }
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Napaka pri nalaganju smeri"
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
}
