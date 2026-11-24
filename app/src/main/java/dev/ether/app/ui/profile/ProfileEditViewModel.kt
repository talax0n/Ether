package dev.ether.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.ProfileRepository
import dev.ether.app.domain.AvatarFace
import dev.ether.app.domain.ContactKind
import dev.ether.app.domain.Contacts
import dev.ether.app.domain.Profile
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
    val skills: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    val phone: String = "",
    val instagram: String = "",
    val lineId: String = "",
    val face: AvatarFace? = null,
    /** The sign-in photo (Google's), shown when no face is picked. Not editable here. */
    val photoUrl: String? = null,
) {
    /** Raw text of one contact field. */
    fun raw(kind: ContactKind): String = when (kind) {
        ContactKind.WHATSAPP -> phone
        ContactKind.INSTAGRAM -> instagram
        ContactKind.LINE -> lineId
    }

    fun withContact(kind: ContactKind, value: String): ProfileForm = when (kind) {
        ContactKind.WHATSAPP -> copy(phone = value)
        ContactKind.INSTAGRAM -> copy(instagram = value)
        ContactKind.LINE -> copy(lineId = value)
    }

    /** The field's normalized value; null when it is blank or invalid. */
    fun normalized(kind: ContactKind): String? = raw(kind).takeIf { it.isNotBlank() }?.let {
        when (kind) {
            ContactKind.WHATSAPP -> Contacts.normalizePhone(it)
            ContactKind.INSTAGRAM -> Contacts.normalizeInstagram(it)
            ContactKind.LINE -> Contacts.normalizeLineId(it)
        }
    }

    /** Filled in but not in a format the database accepts. */
    fun isInvalid(kind: ContactKind): Boolean = raw(kind).isNotBlank() && normalized(kind) == null

    /** At least one contact, and none of the filled-in ones invalid; otherwise null. */
    fun contactsOrNull(): Contacts? {
        if (ContactKind.entries.any(::isInvalid)) return null
        return Contacts(normalized(ContactKind.WHATSAPP), normalized(ContactKind.INSTAGRAM), normalized(ContactKind.LINE))
            .takeUnless { it.isEmpty }
    }

    fun toDraftOrNull(): ProfileDraft? {
        val yearValue = year.trim().toIntOrNull()?.takeIf { it in 1..10 } ?: return null
        // Everything is required: the database only counts a profile as complete with all of it.
        if (name.isBlank() || major.isBlank() || bio.isBlank()) return null
        val contacts = contactsOrNull() ?: return null
        if (skills.isEmpty() || roles.isEmpty()) return null
        return ProfileDraft(name.trim(), major.trim(), yearValue, bio.trim(), skills, roles, contacts, face)
    }

    companion object {
        fun of(p: Profile) = ProfileForm(
            p.name, p.major.orEmpty(), p.year?.toString().orEmpty(), p.bio,
            p.skills, p.roles,
            phone = p.contacts.phone?.let { "+$it" }.orEmpty(),
            instagram = p.contacts.instagram?.let { "@$it" }.orEmpty(),
            lineId = p.contacts.lineId.orEmpty(),
            face = p.face,
            photoUrl = p.avatarUrl,
        )
    }
}


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
                    val form = p?.let(ProfileForm::of) ?: ProfileForm()
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
