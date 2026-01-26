package io.github.samolego.ascendo_trainboard.ui.problems.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.samolego.ascendo_trainboard.api.AscendoApi
import io.github.samolego.ascendo_trainboard.api.generated.models.ProblemSummary
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.api.generated.models.Tag
import io.github.samolego.ascendo_trainboard.ui.components.error.ErrorUiState
import io.github.samolego.ascendo_trainboard.ui.components.error.toErrorUiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MIN_GRADE = 0
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
    val tags: List<Tag> = emptyList(),
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
                tags = _state.value.tags,
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
                tags = _state.value.tags,
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

    fun addTag(tag: Tag) {
        _state.update { state ->
            val newTags = state.tags.toMutableList()

            // Remove existing tag of the same type to override it
            // Use serialization to find the active key (e.g. "Avtor", "SectorId")
            // Default Json configuration omits nulls, so only the set property will be present.
            val activeKey = getTagKey(tag)
            if (activeKey != null) {
                newTags.removeAll { getTagKey(it) == activeKey }
            }

            newTags.add(tag)
            state.copy(tags = newTags)
        }
        loadProblems(refresh = true)
    }

    fun removeTag(tag: Tag) {
        _state.update {
            it.copy(tags = it.tags - tag)
        }
        loadProblems(refresh = true)
    }

    fun setGradeRange(minGrade: Int, maxGrade: Int) {
        _state.update { state ->
            val newTags = state.tags.filter { it.minGrade == null && it.maxGrade == null }.toMutableList()
            if (minGrade > MIN_GRADE) {
                newTags.add(Tag(minGrade = minGrade))
            }
            if (maxGrade < MAX_GRADE) {
                newTags.add(Tag(maxGrade = maxGrade))
            }
            state.copy(tags = newTags)
        }
        loadProblems(refresh = true)
    }

    fun clearFilters() {
        _state.update { it.copy(tags = emptyList()) }
        loadProblems(refresh = true)
    }

    fun refresh() {
        loadSectors()
        loadProblems(refresh = true)
    }

    fun isAuthenticated(): Boolean = api.isAuthenticated()

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

private fun getTagKey(tag: Tag): String? {
    // Encode to JSON tree; default configuration skips nulls.
    // The resulting object should have exactly one key.
    return try {
        Json.encodeToJsonElement(Tag.serializer(), tag).jsonObject.keys.firstOrNull()
    } catch (e: Exception) {
        null
    }
}
