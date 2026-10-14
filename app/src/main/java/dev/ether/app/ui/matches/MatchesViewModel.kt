package dev.ether.app.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.CompetitionRepository
import dev.ether.app.data.MatchRepository
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.Match
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MatchSection(val competitionName: String, val matches: List<Match>)

sealed interface MatchesUiState {
    data object Loading : MatchesUiState
    data class Loaded(val sections: List<MatchSection>) : MatchesUiState {
        val isEmpty get() = sections.all { it.matches.isEmpty() }
    }
    data class Failed(val message: String) : MatchesUiState
}

/** Matches in one competition, or across all of them when [competitionId] is null. */
class MatchesViewModel(
    private val competitionId: CompetitionId?,
    private val repo: MatchRepository,
    private val competitions: CompetitionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<MatchesUiState>(MatchesUiState.Loading)
    val state: StateFlow<MatchesUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value !is MatchesUiState.Loaded) _state.value = MatchesUiState.Loading
        viewModelScope.launch {
            _state.value = runCatching { sections() }
                .fold({ MatchesUiState.Loaded(it) }, { MatchesUiState.Failed(it.userMessage()) })
        }
    }

    private suspend fun sections(): List<MatchSection> = coroutineScope {
        val scope = competitionId?.let { listOf(competitions.competition(it)) } ?: competitions.competitions()
        scope.map { c -> async { MatchSection(c.name, repo.matches(c.id)) } }.awaitAll()
            .filter { it.matches.isNotEmpty() }
    }
}
