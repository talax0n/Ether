package dev.ether.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.ProfileRepository
import dev.ether.app.domain.ProfileDraft
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Form fields are kept as raw text; they become a [ProfileDraft] only when valid. */
data class ProfileForm(
    val name: String = "",
    val major: String = "",
    val year: String = "",
    val bio: String = "",
    val skills: String = "",
    val roles: String = "",
    val contactUrl: String = "",
) {
    fun toDraftOrNull(): ProfileDraft? {
        val yearValue = year.trim().toIntOrNull()?.takeIf { it in 1..10 } ?: return null
        // Everything is required: the database only counts a profile as complete with all of it.
        if (name.isBlank() || major.isBlank() || bio.isBlank() || !contactUrl.trim().startsWith("https://")) return null
        val skillTags = skills.splitTags().ifEmpty { return null }
        val roleTags = roles.splitTags().ifEmpty { return null }
        return ProfileDraft(name.trim(), major.trim(), yearValue, bio.trim(), skillTags, roleTags, contactUrl.trim())
    }
}

private fun String.splitTags() = split(',').map(String::trim).filter(String::isNotEmpty).distinct()

data class ProfileEditUiState(
    val loading: Boolean = true,
    val form: ProfileForm = ProfileForm(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    /** Loading the current profile failed; the form is replaced by a retry message. */
    val loadError: String? = null,
    val error: String? = null,
) {
    val canSave get() = !loading && !saving && form.toDraftOrNull() != null
}

class ProfileEditViewModel(private val profiles: ProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileEditUiState())
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            runCatching { profiles.myProfile() }
                .onSuccess { p ->
                    val form = p?.let {
                        ProfileForm(
                            it.name, it.major.orEmpty(), it.year?.toString().orEmpty(), it.bio,
                            it.skills.joinToString(", "), it.roles.joinToString(", "), it.contactUrl.orEmpty(),
                        )
                    } ?: ProfileForm()
                    _state.update { it.copy(loading = false, form = form) }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, loadError = e.userMessage()) } }
        }
    }

    fun edit(transform: ProfileForm.() -> ProfileForm) = _state.update { it.copy(form = it.form.transform(), error = null) }

    fun save() {
        val draft = _state.value.form.toDraftOrNull() ?: return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching { profiles.saveMyProfile(draft) }
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.userMessage()) } }
        }
    }
}
