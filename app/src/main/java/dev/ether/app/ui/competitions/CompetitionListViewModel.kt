package dev.ether.app.ui.competitions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.CompetitionRepository
import dev.ether.app.domain.Competition
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CompetitionListUiState {
    data object Loading : CompetitionListUiState
    data class Loaded(val competitions: List<Competition>) : CompetitionListUiState
    data class Failed(val message: String) : CompetitionListUiState
}

class CompetitionListViewModel(private val competitions: CompetitionRepository) : ViewModel() {
    private val _state = MutableStateFlow<CompetitionListUiState>(CompetitionListUiState.Loading)
    val state: StateFlow<CompetitionListUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = CompetitionListUiState.Loading
        viewModelScope.launch {
            _state.value = runCatching { competitions.competitions() }
                .fold({ CompetitionListUiState.Loaded(it) }, { CompetitionListUiState.Failed(it.userMessage()) })
        }
    }
}
