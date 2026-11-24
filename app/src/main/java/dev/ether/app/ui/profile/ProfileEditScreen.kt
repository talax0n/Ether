package dev.ether.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import dev.ether.app.domain.TagCatalog
import dev.ether.app.ui.components.TagPicker
import androidx.compose.ui.semantics.contentDescription
import dev.ether.app.domain.AvatarFace
import dev.ether.app.ui.components.Avatar
import dev.ether.app.ui.components.drawable
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.Ink
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.ether.app.ui.components.LedgeIconButton
import dev.ether.app.domain.ContactKind
import dev.ether.app.ui.components.icon
import dev.ether.app.ui.theme.CoralSoft
import dev.ether.app.ui.theme.SkySoft
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.ArcadeTextField
import dev.ether.app.ui.components.Chip
import dev.ether.app.ui.components.XpBar
import dev.ether.app.ui.components.ErrorText
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LoadingBox
import dev.ether.app.ui.components.MessageBox
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.NumberStyle
import dev.ether.app.ui.theme.SunSoft
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileEditScreen(isSetup: Boolean, onDone: () -> Unit, onBack: () -> Unit, vm: ProfileEditViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    val colors = Arcade.colors
    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().imePadding()) {
        if (!isSetup) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                LedgeIconButton(ArcadeIcons.Back, "Back", onBack, size = 42.dp)
            }
        }
        if (state.loading) return@Column LoadingBox()
        state.loadError?.let { return@Column MessageBox(it, action = "Retry", tone = Tone.ERROR, onAction = vm::load) }
        val form = state.form
        val filled = listOf(form.name, form.major, form.year, form.bio).count { it.isNotBlank() } +
            listOf(form.skills, form.roles).count { it.isNotEmpty() } +
            (if (form.contactsOrNull() != null) 1 else 0)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = if (isSetup) 24.dp else 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.staggerIn(0)) {
                Text(
                    if (isSetup) "Set up your\nprofile" else "Edit profile",
                    Modifier.semantics { heading() }, style = MaterialTheme.typography.displaySmall, color = colors.text,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isSetup) "One minute, then you're swiping. This is what teams see on your card." else "This is what teams see on your card.",
                    style = MaterialTheme.typography.bodyLarge, color = colors.muted,
                )
            }
            Row(Modifier.staggerIn(1), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$filled/7", style = NumberStyle, color = colors.text)
                XpBar(filled / 7f, Modifier.weight(1f))
                if (isSetup) Chip("+50 XP", color = SunSoft, icon = ArcadeIcons.Bolt)
            }
            FacePicker(form, Modifier.staggerIn(2)) { vm.edit { copy(face = it) } }
            Field("Name", form.name, 2) { vm.edit { copy(name = it) } }
            Field("Major", form.major, 3) { vm.edit { copy(major = it) } }
            Field("Year (1-10)", form.year, 4, KeyboardType.Number) { vm.edit { copy(year = it) } }
            Field("Bio", form.bio, 5, singleLine = false) { vm.edit { copy(bio = it) } }
            TagPicker("Skills", form.skills, TagCatalog.skills, SkySoft, { vm.edit { copy(skills = it) } }, Modifier.staggerIn(6))
            TagPicker("Roles", form.roles, TagCatalog.roles, CoralSoft, { vm.edit { copy(roles = it) } }, Modifier.staggerIn(7))
            Text("How matches reach you", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.titleMedium, color = colors.text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(ArcadeIcons.Lock, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Add at least one. Only matches and teammates see them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
            ContactKind.entries.forEach { kind -> ContactField(form, kind, 8) { vm.edit { withContact(kind, it) } } }
            state.error?.let { ErrorText(it) }
        }
        LedgeButton(
            if (state.saving) "Saving…" else "Save", vm::save, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            enabled = state.canSave, icon = ArcadeIcons.Check,
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    index: Int,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    ArcadeTextField(
        value = value,
        onValueChange = onChange,
        label = label,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.staggerIn(index),
    )
}

private val ContactKind.fieldLabel get() = when (this) {
    ContactKind.WHATSAPP -> "Phone (WhatsApp)"
    ContactKind.INSTAGRAM -> "Instagram"
    ContactKind.LINE -> "LINE ID"
}

private val ContactKind.hint get() = when (this) {
    ContactKind.WHATSAPP -> "Your WhatsApp number, e.g. 0812 3456 7890 or +62 812 3456 7890."
    ContactKind.INSTAGRAM -> "Your handle, e.g. @ana.designs."
    ContactKind.LINE -> "Your LINE ID (Settings › Profile), not your display name."
}

/** Mirrors the profile_contacts check constraints. */
private val ContactKind.formatError get() = when (this) {
    ContactKind.WHATSAPP -> "Use 8 to 16 digits, with the country code or a leading 0."
    ContactKind.INSTAGRAM -> "Up to 30 letters, numbers, dots or underscores."
    ContactKind.LINE -> "4 to 20 letters, numbers, dots, underscores or hyphens."
}

@Composable
private fun ContactField(form: ProfileForm, kind: ContactKind, index: Int, onChange: (String) -> Unit) {
    val colors = Arcade.colors
    Column(Modifier.staggerIn(index), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ArcadeTextField(
            value = form.raw(kind),
            onValueChange = onChange,
            label = kind.fieldLabel,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (kind == ContactKind.WHATSAPP) KeyboardType.Phone else KeyboardType.Ascii,
                autoCorrectEnabled = false,
            ),
            trailingIcon = { Icon(kind.icon, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp)) },
        )
        val value = form.normalized(kind)
        when {
            form.isInvalid(kind) -> ErrorText(kind.formatError, Modifier.padding(horizontal = 4.dp))
            value != null -> Text(
                "Opens ${kind.url(value).removePrefix("https://")}",
                Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.bodySmall, color = colors.muted,
            )
            else -> Text(kind.hint, Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
    }
}

/** Your picture: a picked face, else your photo or initials. "Choose a face" opens a grid of all faces. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FacePicker(form: ProfileForm, modifier: Modifier = Modifier, onPick: (AvatarFace?) -> Unit) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    val colors = Arcade.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Your picture", style = MaterialTheme.typography.titleMedium, color = colors.text)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(form.name.ifBlank { "?" }, form.photoUrl, 76.dp, ledge = 4.dp, color = SunSoft, face = form.face?.drawable)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Choose a face",
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { choosing = true }.heightIn(min = 40.dp).wrapContentHeight().padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.titleSmall, color = colors.text, textDecoration = TextDecoration.Underline,
                )
                if (form.face != null) {
                    Text(
                        if (form.photoUrl != null) "Use my photo" else "Use my initials",
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { onPick(null) }.heightIn(min = 40.dp).wrapContentHeight().padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.bodyMedium, color = colors.muted,
                    )
                }
            }
        }
    }
    if (choosing) {
        ModalBottomSheet(onDismissRequest = { choosing = false }, containerColor = colors.background) {
            Text("Choose a face", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge, color = colors.text)
            LazyVerticalGrid(
                GridCells.Adaptive(76.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(AvatarFace.entries) { face ->
                    FaceOption(form.face == face, face.name.lowercase().replaceFirstChar { it.uppercase() }, {
                        onPick(face)
                        choosing = false
                    }) {
                        Avatar("", null, 60.dp, ledge = 3.dp, color = SkySoft, face = face.drawable)
                    }
                }
            }
        }
    }
}

@Composable
private fun FaceOption(selected: Boolean, label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label }
            .border(3.dp, if (selected) Coral else Color.Transparent, CircleShape)
            .padding(5.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        content()
        if (selected) {
            Icon(
                ArcadeIcons.Check, contentDescription = null, tint = Ink,
                modifier = Modifier.size(20.dp).background(Coral, CircleShape).padding(3.dp),
            )
        }
    }
}
