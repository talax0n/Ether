package dev.ether.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.AuthRepository
import dev.ether.app.data.LevelMemory
import dev.ether.app.data.ProfileRepository
import dev.ether.app.data.TrackRecordRepository
import dev.ether.app.domain.PastHackathon
import dev.ether.app.domain.PastHackathonId
import dev.ether.app.domain.Project
import dev.ether.app.domain.ProjectId
import dev.ether.app.domain.ProfileSummary
import dev.ether.app.domain.TrackRecord
import dev.ether.app.domain.leveledUp
import dev.ether.app.domain.levelFor
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Year

/** The open add/edit sheet for one track-record item. [editing] is null when adding. */
sealed interface TrackRecordEditor {
    val saving: Boolean
    val error: String?
    /** The first delete tap arms it; the second deletes. */
    val confirmingDelete: Boolean

    data class Hackathon(
        val editing: PastHackathonId?,
        val form: HackathonForm,
        override val saving: Boolean = false,
        override val error: String? = null,
        override val confirmingDelete: Boolean = false,
    ) : TrackRecordEditor

    data class Project(
        val editing: ProjectId?,
        val form: ProjectForm,
        override val saving: Boolean = false,
        override val error: String? = null,
        override val confirmingDelete: Boolean = false,
    ) : TrackRecordEditor
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    /** [leveledUp] is true the first time Profile shows a level higher than the last one this device saw. */
    data class Loaded(
        val summary: ProfileSummary,
        val trackRecord: TrackRecord,
        val leveledUp: Boolean = false,
        val editor: TrackRecordEditor? = null,
        /** The year that bounds the hackathon year field (it allows up to next year). */
        val currentYear: Int,
    ) : ProfileUiState {
        val canSave: Boolean
            get() = when (val e = editor) {
                null -> false
                is TrackRecordEditor.Hackathon -> !e.saving && e.form.toDraftOrNull(currentYear) != null
                is TrackRecordEditor.Project -> !e.saving && e.form.toDraftOrNull() != null
            }
    }
    data class Failed(val message: String) : ProfileUiState
}

class ProfileViewModel(
    private val profiles: ProfileRepository,
    private val trackRecords: TrackRecordRepository,
    private val auth: AuthRepository,
    private val device: LevelMemory,
    private val currentYear: () -> Int = { Year.now().value },
) : ViewModel() {
    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load() {
        // A reload on resume keeps the loaded screen (and any open sheet) instead of flashing a spinner.
        val previous = _state.value as? ProfileUiState.Loaded
        if (previous == null) _state.value = ProfileUiState.Loading
        viewModelScope.launch {
            _state.value = runCatching {
                coroutineScope {
                    val summary = async { profiles.mySummary() }
                    val record = async { trackRecords.myTrackRecord() }
                    summary.await() to record.await()
                }
            }.fold(
                { (summary, record) ->
                    val level = levelFor(summary.profile.xp).number
                    val up = leveledUp(device.lastSeenLevel, level)
                    device.lastSeenLevel = level
                    val current = _state.value as? ProfileUiState.Loaded
                    ProfileUiState.Loaded(
                        summary, record, leveledUp = up,
                        editor = current?.editor, currentYear = currentYear(),
                    )
                },
                { e -> previous ?: ProfileUiState.Failed(e.userMessage()) },
            )
        }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    // ---------------------------------------------------------------- track record editor

    fun addHackathon() = open { TrackRecordEditor.Hackathon(null, HackathonForm.new(it.currentYear)) }
    fun editHackathon(h: PastHackathon) = open { TrackRecordEditor.Hackathon(h.id, HackathonForm.of(h)) }
    fun addProject() = open { TrackRecordEditor.Project(null, ProjectForm()) }
    fun editProject(p: Project) = open { TrackRecordEditor.Project(p.id, ProjectForm.of(p)) }

    fun editHackathonForm(form: HackathonForm) = updateEditor { e ->
        (e as? TrackRecordEditor.Hackathon)?.copy(form = form, error = null, confirmingDelete = false) ?: e
    }

    fun editProjectForm(form: ProjectForm) = updateEditor { e ->
        (e as? TrackRecordEditor.Project)?.copy(form = form, error = null, confirmingDelete = false) ?: e
    }

    fun closeEditor() = updateLoaded { if (it.editor?.saving == true) it else it.copy(editor = null) }

    fun save() {
        val loaded = _state.value as? ProfileUiState.Loaded ?: return
        if (!loaded.canSave) return
        val action: suspend () -> Unit = when (val e = loaded.editor) {
            is TrackRecordEditor.Hackathon -> {
                val draft = e.form.toDraftOrNull(loaded.currentYear) ?: return
                val id = e.editing
                if (id == null) suspend { trackRecords.addHackathon(draft) } else suspend { trackRecords.updateHackathon(id, draft) }
            }
            is TrackRecordEditor.Project -> {
                val draft = e.form.toDraftOrNull() ?: return
                val id = e.editing
                if (id == null) suspend { trackRecords.addProject(draft) } else suspend { trackRecords.updateProject(id, draft) }
            }
            null -> return
        }
        write(action)
    }

    /** First call arms the delete; the second call deletes the item being edited. */
    fun delete() {
        val e = (_state.value as? ProfileUiState.Loaded)?.editor ?: return
        if (e.saving) return
        val action: suspend () -> Unit = when (e) {
            is TrackRecordEditor.Hackathon -> {
                val id = e.editing ?: return
                if (!e.confirmingDelete) return updateEditor { e.copy(confirmingDelete = true) }
                suspend { trackRecords.deleteHackathon(id) }
            }
            is TrackRecordEditor.Project -> {
                val id = e.editing ?: return
                if (!e.confirmingDelete) return updateEditor { e.copy(confirmingDelete = true) }
                suspend { trackRecords.deleteProject(id) }
            }
        }
        write(action)
    }

    /** Runs a write, then re-reads the track record so the list shows exactly what the database kept. */
    private fun write(action: suspend () -> Unit) {
        updateEditor { it.withProgress(saving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                action()
                trackRecords.myTrackRecord()
            }.onSuccess { record ->
                updateLoaded { it.copy(trackRecord = record, editor = null) }
            }.onFailure { err ->
                updateEditor { it.withProgress(saving = false, error = err.userMessage()) }
            }
        }
    }

    private fun open(editor: (ProfileUiState.Loaded) -> TrackRecordEditor) = updateLoaded { it.copy(editor = editor(it)) }

    private fun updateLoaded(transform: (ProfileUiState.Loaded) -> ProfileUiState.Loaded) {
        val s = _state.value
        if (s is ProfileUiState.Loaded) _state.value = transform(s)
    }

    private fun updateEditor(transform: (TrackRecordEditor) -> TrackRecordEditor) =
        updateLoaded { s -> s.editor?.let { s.copy(editor = transform(it)) } ?: s }

    private fun TrackRecordEditor.withProgress(saving: Boolean, error: String?): TrackRecordEditor = when (this) {
        is TrackRecordEditor.Hackathon -> copy(saving = saving, error = error, confirmingDelete = false)
        is TrackRecordEditor.Project -> copy(saving = saving, error = error, confirmingDelete = false)
    }
}
