package io.github.samolego.ascendo_trainboard.ui.problems.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.samolego.ascendo_trainboard.api.AscendoApi
import io.github.samolego.ascendo_trainboard.api.generated.models.ProblemSummary
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.ui.components.error.ErrorUiState
import io.github.samolego.ascendo_trainboard.ui.components.error.toErrorUiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MIN_GRADE = 1
const val MAX_GRADE = 16


data class ProblemListState(
    val problems: List<ProblemSummary> = emptyList(),
    val sectors: List<SectorSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingSectors: Boolean = false,
    val error: ErrorUiState? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val selectedSector: SectorSummary? = null,
    val minGrade: Int = MIN_GRADE,
    val maxGrade: Int = MAX_GRADE,
    val searchAuthor: String = ""
)

@OptIn(FlowPreview::class)
class ProblemListViewModel(
    private val api: AscendoApi
) : ViewModel() {

    private val _state = MutableStateFlow(ProblemListState())
    val state: StateFlow<ProblemListState> = _state.asStateFlow()

    init {
        loadSectors()
        loadProblems()

        _state
            .map { it.searchAuthor }
            .distinctUntilChanged()
            .debounce(500L)
            .onEach {
                loadProblems(refresh = true)
            }
            .launchIn(viewModelScope)
    }

    private fun loadSectors() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingSectors = true) }

            val result = api.getSectors()

            result.onSuccess { sectors ->
                _state.update {
                    it.copy(
                        sectors = sectors,
                        isLoadingSectors = false
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoadingSectors = false,
                        error = error.toErrorUiState()
                    )
                }
            }
        }
    }

    fun loadProblems(refresh: Boolean = false) {
        println("Loading problems, refresh=$refresh")
        viewModelScope.launch {
            if (refresh) {
                _state.update { it.copy(isLoading = true, currentPage = 1, problems = emptyList()) }
            } else {
                _state.update { it.copy(isLoading = true) }
            }

            val result = api.getProblems(
                sector = _state.value.selectedSector?.id,
                minGrade = _state.value.minGrade,
                maxGrade = _state.value.maxGrade,
                name = _state.value.searchAuthor.ifBlank { null },
                page = 1,
                perPage = 20
            )

            result.onSuccess { problemList ->
                _state.update {
                    it.copy(
                        problems = problemList.problems,
                        isLoading = false,
                        error = null,
                        currentPage = 1,
                        hasMore = problemList.problems.size < problemList.total
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.toErrorUiState()
                    )
                }
            }
        }
    }

    fun loadMore() {
        if (_state.value.isLoadingMore || !_state.value.hasMore) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }

            val nextPage = _state.value.currentPage + 1
            val result = api.getProblems(
                sector = _state.value.selectedSector?.id,
                minGrade = _state.value.minGrade,
                maxGrade = _state.value.maxGrade,
                name = _state.value.searchAuthor.ifBlank { null },
                page = nextPage,
                perPage = 20
            )

            result.onSuccess { problemList ->
                _state.update {
                    it.copy(
                        problems = it.problems + problemList.problems,
                        isLoadingMore = false,
                        currentPage = nextPage,
                        hasMore = (it.problems.size + problemList.problems.size) < problemList.total
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        error = error.toErrorUiState()
                    )
                }
            }
        }
    }

    fun setSectorFilter(sector: SectorSummary?) {
        _state.update { it.copy(selectedSector = sector) }
        loadProblems(refresh = true)
    }

    fun setGradeRange(minGrade: Int, maxGrade: Int) {
        _state.update { it.copy(minGrade = minGrade, maxGrade = maxGrade) }
        loadProblems(refresh = true)
    }

    fun setAuthorSearch(author: String) {
        _state.update { it.copy(searchAuthor = author) }
    }

    fun clearFilters() {
        _state.update {
            it.copy(
                selectedSector = null,
                minGrade = MIN_GRADE,
                maxGrade = MAX_GRADE,
                searchAuthor = ""
            )
        }
        loadProblems(refresh = true)
    }

    fun refresh() {
        loadProblems(refresh = true)
    }

    fun isAuthenticated(): Boolean = api.isAuthenticated()

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
