package dev.ether.app.ui.competitions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.CompetitionRepository
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionMode
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

sealed interface CompetitionListUiState {
    data object Loading : CompetitionListUiState
    data class Loaded(
        val competitions: List<Competition>,
        val filters: CompetitionFilters = CompetitionFilters(),
        val now: Instant = Instant.now(),
    ) : CompetitionListUiState {
        val results: List<Competition> = competitions.filter { filters.matches(it, now) }
        val sections = results.bySection(now)
        val tags: List<String> = competitions.availableTags()
        /** Counted over everything loaded, so the header doesn't change as you filter. */
        val openCount: Int = competitions.count { it.status(now) == EventStatus.OPEN }
    }
    data class Failed(val message: String) : CompetitionListUiState
}

class CompetitionListViewModel(
    private val competitions: CompetitionRepository,
    private val clock: () -> Instant = Instant::now,
) : ViewModel() {
    private val _state = MutableStateFlow<CompetitionListUiState>(CompetitionListUiState.Loading)
    val state: StateFlow<CompetitionListUiState> = _state.asStateFlow()

    /** Kept across a reload, so retrying after an error doesn't wipe the search. */
    private var filters = CompetitionFilters()

    init { load() }

    fun load() {
        _state.value = CompetitionListUiState.Loading
        viewModelScope.launch {
            _state.value = runCatching { competitions.competitions() }
                .fold({ CompetitionListUiState.Loaded(it, filters, clock()) }, { CompetitionListUiState.Failed(it.userMessage()) })
        }
    }

    fun setQuery(query: String) = edit { it.copy(query = query) }
    fun toggleStatus(status: EventStatus) = edit { it.toggle(status) }
    fun toggleMode(mode: CompetitionMode) = edit { it.toggle(mode) }
    fun toggleTag(tag: String) = edit { it.toggleTag(tag) }
    fun clearFilters() = edit { CompetitionFilters() }

    private fun edit(change: (CompetitionFilters) -> CompetitionFilters) {
        filters = change(filters)
        _state.update { if (it is CompetitionListUiState.Loaded) it.copy(filters = filters) else it }
    }
}
