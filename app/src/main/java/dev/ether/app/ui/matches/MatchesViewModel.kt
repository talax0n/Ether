package dev.ether.app.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.MatchRepository
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.Match
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MatchesUiState {
    data object Loading : MatchesUiState
    data class Loaded(val matches: List<Match>) : MatchesUiState
    data class Failed(val message: String) : MatchesUiState
}

class MatchesViewModel(private val competitionId: CompetitionId, private val repo: MatchRepository) : ViewModel() {
    private val _state = MutableStateFlow<MatchesUiState>(MatchesUiState.Loading)
    val state: StateFlow<MatchesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = MatchesUiState.Loading
        viewModelScope.launch {
            _state.value = runCatching { repo.matches(competitionId) }
                .fold({ MatchesUiState.Loaded(it) }, { MatchesUiState.Failed(it.userMessage()) })
        }
    }
}
