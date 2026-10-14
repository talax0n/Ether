package dev.ether.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.R
import dev.ether.app.domain.ProfileSummary
import dev.ether.app.ui.Avatar
import dev.ether.app.ui.LoadingBox
import dev.ether.app.ui.MessageBox
import dev.ether.app.ui.TagRow
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onEdit: () -> Unit, onBack: () -> Unit, vm: ProfileViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    // Reload on every resume so edits made on the edit screen show up when navigating back.
    LifecycleResumeEffect(Unit) {
        vm.load()
        onPauseOrDispose {}
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") } },
                actions = { TextButton(onClick = onEdit) { Text("Edit") } },
            )
        },
    ) { padding ->
        when (val s = state) {
            ProfileUiState.Loading -> LoadingBox(Modifier.padding(padding))
            is ProfileUiState.Failed -> MessageBox(s.message, Modifier.padding(padding), "Retry", vm::load)
            is ProfileUiState.Loaded -> ProfileContent(s.summary, vm::signOut, Modifier.padding(padding))
        }
    }
}

@Composable
private fun ProfileContent(summary: ProfileSummary, onSignOut: () -> Unit, modifier: Modifier) {
    val p = summary.profile
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(p.name, p.avatarUrl, 96.dp)
        Text(p.name, style = MaterialTheme.typography.headlineMedium)
        listOfNotNull(p.major, p.year?.let { "Year $it" }).takeIf { it.isNotEmpty() }?.let {
            Text(it.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("XP", p.xp.toString(), Modifier.weight(1f))
            Stat("Endorsements", summary.reliability.endorsements.toString(), Modifier.weight(1f))
            Stat("Badges", summary.badges.size.toString(), Modifier.weight(1f))
        }
        if (p.bio.isNotBlank()) Text(p.bio, Modifier.fillMaxWidth())
        Section("Skills") { TagRow(p.skills) }
        Section("Roles") { TagRow(p.roles, container = MaterialTheme.colorScheme.tertiaryContainer) }
        Section("Badges") {
            if (summary.badges.isEmpty()) {
                Text("Form a team or finish an event to earn your first badge.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            summary.badges.forEach { b ->
                Text(b.name, style = MaterialTheme.typography.titleSmall)
                Text(b.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedButton(onClick = onSignOut) { Text("Sign out") }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}
