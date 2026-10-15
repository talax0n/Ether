package dev.ether.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import dev.ether.app.ui.components.ArcadeTopBar
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
    Column(Modifier.fillMaxSize().background(Arcade.colors.background)) {
        ArcadeTopBar(
            title = if (isSetup) "Set up your profile" else "Edit profile",
            subtitle = if (isSetup) "One minute, then you're swiping" else null,
            onBack = if (isSetup) null else onBack,
        )
        if (state.loading) return@Column LoadingBox()
        state.loadError?.let { return@Column MessageBox(it, action = "Retry", tone = Tone.ERROR, onAction = vm::load) }
        val form = state.form
        val filled = listOf(form.name, form.major, form.year, form.bio, form.skills, form.roles, form.contactUrl).count { it.isNotBlank() }
        Column(
            Modifier.imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.staggerIn(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$filled/7", style = NumberStyle, color = Arcade.colors.text)
                XpBar(filled / 7f, Modifier.weight(1f))
                if (isSetup) Chip("+50 XP", color = SunSoft, icon = ArcadeIcons.Bolt)
            }
            Field("Name", form.name, 1) { vm.edit { copy(name = it) } }
            Field("Major", form.major, 2) { vm.edit { copy(major = it) } }
            Field("Year (1-10)", form.year, 3, KeyboardType.Number) { vm.edit { copy(year = it) } }
            Field("Bio", form.bio, 4, singleLine = false) { vm.edit { copy(bio = it) } }
            Field("Skills, comma separated", form.skills, 5) { vm.edit { copy(skills = it) } }
            Field("Roles, comma separated", form.roles, 6) { vm.edit { copy(roles = it) } }
            Field("Contact link (https://wa.me/… or https://t.me/…)", form.contactUrl, 7, KeyboardType.Uri) {
                vm.edit { copy(contactUrl = it) }
            }
            Text(
                "All fields are required. Only your teammates and people you match with can see your contact link.",
                style = MaterialTheme.typography.bodySmall,
                color = Arcade.colors.muted,
            )
            state.error?.let { ErrorText(it) }
            LedgeButton(
                if (state.saving) "Saving…" else "Save", vm::save, Modifier.fillMaxWidth().padding(bottom = 16.dp),
                enabled = state.canSave, icon = ArcadeIcons.Check,
            )
        }
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
