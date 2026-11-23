package dev.ether.app.ui.nearby

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.ArcadeSnackbarHost
import dev.ether.app.ui.components.Avatar
import dev.ether.app.ui.components.Chip
import dev.ether.app.ui.components.EthaLive
import dev.ether.app.ui.components.EthaMove
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LedgeCard
import dev.ether.app.ui.components.LedgeIconButton
import dev.ether.app.ui.components.LoadingBox
import dev.ether.app.ui.components.MessageBox
import dev.ether.app.ui.components.SectionHeader
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.components.performOrYield
import dev.ether.app.ui.components.rememberEthaController
import dev.ether.app.ui.deck.MatchCelebration
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.Mint
import dev.ether.app.ui.theme.MintSoft
import dev.ether.app.ui.theme.Sky
import dev.ether.app.ui.theme.SkySoft
import dev.ether.app.ui.theme.Sun
import dev.ether.app.ui.theme.SunSoft
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.sin
import org.koin.core.parameter.parametersOf

/** What Nearby Connections needs at runtime, per API level (minSdk 26, targetSdk 36). */
fun nearbyPermissions(sdk: Int = Build.VERSION.SDK_INT): List<String> = buildList {
    if (sdk >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_ADVERTISE)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    // Wi-Fi discovery: its own permission from 13, location before that.
    if (sdk >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.NEARBY_WIFI_DEVICES)
    else add(Manifest.permission.ACCESS_FINE_LOCATION)
}

private fun Context.hasNearbyPermissions() =
    nearbyPermissions().all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

@Composable
fun NearbyScreen(
    competitionId: String,
    listingId: String,
    onOpenTeams: () -> Unit,
    onBack: () -> Unit,
    vm: NearbyViewModel = koinViewModel { parametersOf(competitionId, listingId) },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        vm.onPermissions(results.values.all { it })
    }
    val askPermissions = { request.launch(nearbyPermissions().toTypedArray()) }
    // Nearby and the accelerometer run only while the screen is started. Permissions are re-checked on every
    // start, so coming back from Settings picks up a grant.
    LifecycleStartEffect(vm) {
        if (context.hasNearbyPermissions()) vm.onPermissions(true)
        vm.onVisible(true)
        onStopOrDispose { vm.onVisible(false) }
    }
    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbar.showSnackbar(it)
            vm.noticeShown()
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                LedgeIconButton(ArcadeIcons.Back, "Back", onBack, size = 42.dp)
                Text(
                    "Match nearby",
                    Modifier.padding(horizontal = 14.dp).semantics { heading() },
                    style = MaterialTheme.typography.titleLarge, color = Arcade.colors.text,
                )
            }
            val body = Modifier.weight(1f)
            when (state.phase) {
                NearbyPhase.LOADING -> LoadingBox(body)
                NearbyPhase.NEEDS_PERMISSION -> PermissionPrompt(
                    EthaMove.WAVE, "Find teammates in the room",
                    "Ether uses Bluetooth and Wi-Fi to find people near you who have this screen open. " +
                        "It doesn't track or store where you are.",
                    "Allow nearby devices", askPermissions, body,
                )
                NearbyPhase.PERMISSION_DENIED -> PermissionPrompt(
                    EthaMove.SHRUG, "Nearby is off",
                    "Without the nearby devices permission, Ether can't see who's around. You can still swipe in the deck.",
                    "Try again", askPermissions, body,
                    secondary = "Open settings" to {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )
                NearbyPhase.UNAVAILABLE -> MessageBox(
                    state.error ?: "Match nearby isn't available right now.", body, "Try again", Tone.ERROR, onAction = vm::retry,
                )
                NearbyPhase.SEARCHING -> Searching(state, vm::shakeByTap, body)
            }
        }
        ArcadeSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    state.match?.let {
        // Every nearby match puts the user on a team (a solo joins one, or two solos form one), so point there.
        MatchCelebration(it, canKeepSwiping = false, onDismiss = vm::dismissMatch, onOpenTeams = onOpenTeams)
    }
}

@Composable
private fun PermissionPrompt(
    move: EthaMove,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EthaLive(size = 150.dp, first = move, pool = EthaMove.FIDGETS)
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, color = Arcade.colors.text, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.titleMedium, color = Arcade.colors.muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        val wide = Modifier.widthIn(max = 320.dp).fillMaxWidth()
        LedgeButton(action, onAction, wide, color = Coral, icon = ArcadeIcons.Nearby)
        secondary?.let { (label, go) -> LedgeButton(label, go, wide, color = Arcade.colors.surface, contentColor = Arcade.colors.text) }
    }
}

@Composable
private fun Searching(state: NearbyUiState, onTapShake: () -> Unit, modifier: Modifier = Modifier) {
    val etha = rememberEthaController()
    // Etha reacts to what's happening: a hop on your shake, a wave when someone turns up.
    LaunchedEffect(state.shookRecently) { if (state.shookRecently) etha.performOrYield(EthaMove.HOP) }
    LaunchedEffect(state.people.size) { if (state.people.isNotEmpty()) etha.performOrYield(EthaMove.WAVE) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
            Pulse(Modifier.size(190.dp))
            EthaLive(size = 130.dp, controller = etha)
        }
        Text(
            "Shake your phone at the same time as your teammate-to-be",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge, color = Arcade.colors.text, textAlign = TextAlign.Center,
        )
        Text(
            "If you both shake within 3 seconds, it counts as a like from each of you. Same rules as the deck.",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium, color = Arcade.colors.muted, textAlign = TextAlign.Center,
        )
        ShakeHint(state, onTapShake)
        SectionHeader("Nearby", trailing = if (state.people.isEmpty()) null else "${state.people.size} here")
        if (state.people.isEmpty()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                PulseDot()
                Spacer(Modifier.width(10.dp))
                Text(
                    "Looking for people nearby with this screen open…",
                    style = MaterialTheme.typography.bodyMedium, color = Arcade.colors.muted,
                )
            }
        } else {
            state.people.forEach { PersonRow(it) }
        }
    }
}

/** The big call to action. It also takes a tap, for anyone who can't shake a phone. */
@Composable
private fun ShakeHint(state: NearbyUiState, onTap: () -> Unit) {
    val reduced = Arcade.reducedMotion
    val pairing = state.pairingWith
    val cycle = if (reduced || pairing != null) null else rememberInfiniteTransition(label = "shake-hint")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(1_600, easing = LinearEasing)), label = "wiggle")
    val (title, body, fill) = when {
        pairing != null -> Triple("Matching with $pairing…", "Recording your like. Hold on a second.", MintSoft)
        state.shookRecently -> Triple("Shaken!", "Waiting for them to shake too.", SkySoft)
        else -> Triple("Shake now", "Can't shake? Tap here instead.", SunSoft)
    }
    LedgeCard(Modifier.fillMaxWidth(), color = fill, onClick = onTap) {
        Row(
            Modifier.padding(18.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                ArcadeIcons.Nearby, contentDescription = null, tint = Ink,
                modifier = Modifier.size(44.dp).graphicsLayer {
                    // Wiggle in bursts: three quick shakes, then a rest.
                    val t = cycle?.value ?: 1f
                    rotationZ = if (t < 0.4f) sin(t / 0.4f * 6f * PI.toFloat()) * 14f else 0f
                },
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Ink)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = Ink)
            }
        }
    }
}

@Composable
private fun PersonRow(person: NearbyPerson) {
    val peer = person.peer
    LedgeCard(Modifier.fillMaxWidth(), ledge = 4.dp) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(peer.name, url = null, size = 44.dp, color = if (person.shook) Mint else SkySoft)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(peer.name, style = MaterialTheme.typography.titleMedium, color = Arcade.colors.text)
                peer.role?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Arcade.colors.muted) }
            }
            if (person.shook) Chip("Shook!", color = Sun, icon = ArcadeIcons.Bolt)
        }
    }
}

/** Rings that grow out from Etha while searching. One still ring under reduced motion. */
@Composable
private fun Pulse(modifier: Modifier) {
    val reduced = Arcade.reducedMotion
    val progress = if (reduced) null else rememberInfiniteTransition(label = "pulse")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(2_000, easing = LinearEasing)), label = "ring")
    val color = Sky
    Canvas(modifier) {
        val max = size.minDimension / 2
        val p = progress?.value
        if (p == null) {
            drawCircle(color.copy(alpha = 0.35f), radius = max * 0.85f, style = Stroke(3.dp.toPx()))
            return@Canvas
        }
        // Two rings half a cycle apart, fading as they grow.
        listOf(p, (p + 0.5f) % 1f).forEach { t ->
            drawCircle(color.copy(alpha = (1f - t) * 0.5f), radius = max * (0.45f + 0.55f * t), style = Stroke(3.dp.toPx()))
        }
    }
}

@Composable
private fun PulseDot() {
    val reduced = Arcade.reducedMotion
    val alpha = if (reduced) null else rememberInfiniteTransition(label = "dot")
        .animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "alpha")
    Canvas(Modifier.size(12.dp).graphicsLayer { this.alpha = alpha?.value ?: 1f }) { drawCircle(Sky) }
}
