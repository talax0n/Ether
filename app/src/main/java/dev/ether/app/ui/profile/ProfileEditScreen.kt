package dev.ether.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.R
import dev.ether.app.ui.LoadingBox
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(isSetup: Boolean, onDone: () -> Unit, onBack: () -> Unit, vm: ProfileEditViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSetup) "Set up your profile" else "Edit profile") },
                navigationIcon = {
                    if (!isSetup) IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        val form = state.form
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Field("Name", form.name) { vm.edit { copy(name = it) } }
            Field("Major", form.major) { vm.edit { copy(major = it) } }
            Field("Year (1-10)", form.year, KeyboardType.Number) { vm.edit { copy(year = it) } }
            Field("Bio", form.bio, singleLine = false) { vm.edit { copy(bio = it) } }
            Field("Skills, comma separated", form.skills) { vm.edit { copy(skills = it) } }
            Field("Roles, comma separated", form.roles) { vm.edit { copy(roles = it) } }
            Field("Contact link (https://wa.me/… or https://t.me/…)", form.contactUrl, KeyboardType.Uri) {
                vm.edit { copy(contactUrl = it) }
            }
            Text(
                "Only people you match with can see your contact link.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = vm::save, enabled = state.canSave, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (state.saving) "Saving…" else "Save")
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}
