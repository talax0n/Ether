package dev.ether.app.ui.competitions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.CompetitionRepository
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.Listing
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeamForm(val name: String = "", val pitch: String = "", val openRoles: String = "") {
    val roles get() = openRoles.split(',').map(String::trim).filter(String::isNotEmpty).distinct()
    val isValid get() = name.isNotBlank() && pitch.isNotBlank() && roles.isNotEmpty()
}

data class CompetitionDetailUiState(
    val competition: Competition? = null,
    val listing: Listing? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    /** Non-null while the create-team form is open. */
    val teamForm: TeamForm? = null,
    val error: String? = null,
)

class CompetitionDetailViewModel(
    private val id: CompetitionId,
    private val competitions: CompetitionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CompetitionDetailUiState())
    val state: StateFlow<CompetitionDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val competition = async { competitions.competition(id) }
                val listing = async { competitions.myListing(id) }
                competition.await() to listing.await()
            }.onSuccess { (c, l) -> _state.update { it.copy(loading = false, competition = c, listing = l) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }
        }
    }

    fun joinSolo() = mutate { competitions.joinSolo(id) }

    fun openTeamForm() = _state.update { it.copy(teamForm = TeamForm()) }
    fun closeTeamForm() = _state.update { it.copy(teamForm = null) }
    fun editTeamForm(form: TeamForm) = _state.update { it.copy(teamForm = form) }

    fun createTeam() {
        val form = _state.value.teamForm?.takeIf { it.isValid } ?: return
        mutate { competitions.createTeam(id, form.name.trim(), form.pitch.trim(), form.roles) }
    }

    private fun mutate(block: suspend () -> Listing) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { l -> _state.update { it.copy(busy = false, listing = l, teamForm = null) } }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.userMessage()) } }
        }
    }
}
