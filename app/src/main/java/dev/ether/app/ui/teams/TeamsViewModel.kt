package dev.ether.app.ui.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.TeamRepository
import dev.ether.app.domain.MyTeam
import dev.ether.app.domain.TeamResult
import dev.ether.app.domain.Teammate
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** [eventEnded] unlocks endorsements and result reporting, which the database only allows after `event_end`. */
data class TeamView(val team: MyTeam, val eventEnded: Boolean)

sealed interface TeamsUiState {
    data object Loading : TeamsUiState
    data class Failed(val message: String) : TeamsUiState
    data class Loaded(
        val teams: List<TeamView>,
        val busy: Boolean = false,
        /** One-off feedback from the last action (an error or a confirmation), shown in a snackbar. */
        val message: String? = null,
    ) : TeamsUiState
}

class TeamsViewModel(
    private val repo: TeamRepository,
    private val clock: () -> Instant = Instant::now,
) : ViewModel() {
    private val _state = MutableStateFlow<TeamsUiState>(TeamsUiState.Loading)
    val state: StateFlow<TeamsUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value !is TeamsUiState.Loaded) _state.value = TeamsUiState.Loading
        viewModelScope.launch {
            _state.value = runCatching { repo.myTeams() }
                .fold({ TeamsUiState.Loaded(it.toViews()) }, { TeamsUiState.Failed(it.userMessage()) })
        }
    }

    fun endorse(team: TeamView, teammate: Teammate) =
        act("You endorsed ${teammate.name}.") { repo.endorse(team.team.competition.id, teammate.id) }

    fun reportResult(team: TeamView, result: TeamResult) =
        act("Reported. A teammate needs to confirm it.") { repo.reportResult(team.team.id, result) }

    fun confirmResult(team: TeamView) = act("Result confirmed.") { repo.confirmResult(team.team.id) }

    fun messageShown() = _state.update { if (it is TeamsUiState.Loaded) it.copy(message = null) else it }

    private fun act(success: String, block: suspend () -> Unit) {
        val loaded = _state.value as? TeamsUiState.Loaded ?: return
        if (loaded.busy) return
        _state.value = loaded.copy(busy = true, message = null)
        viewModelScope.launch {
            // Reload after the action either way: the database decides what changed (XP, badges, results).
            val message = runCatching { block() }.fold({ success }, { it.userMessage() })
            val teams = runCatching { repo.myTeams().toViews() }.getOrDefault(loaded.teams)
            _state.value = TeamsUiState.Loaded(teams, busy = false, message = message)
        }
    }

    private fun List<MyTeam>.toViews(): List<TeamView> {
        val now = clock()
        return map { TeamView(it, eventEnded = it.competition.eventEnd <= now) }
    }
}
