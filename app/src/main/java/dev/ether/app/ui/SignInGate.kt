package dev.ether.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.EthaLive
import dev.ether.app.ui.components.EthaMove
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.theme.Arcade

/**
 * Non-null while browsing as a guest: calling it opens sign-in. Null once signed in.
 * Guests browse competitions freely; anything that needs an account goes through this.
 */
val LocalSignInGate = staticCompositionLocalOf<(() -> Unit)?> { null }

/** [action] for a signed-in user; the sign-in prompt for a guest. */
@Composable
fun gated(action: () -> Unit): () -> Unit = LocalSignInGate.current ?: action

/** Stands in for a tab that needs an account: what the tab is for, and the way in. */
@Composable
fun GuestGate(title: String, body: String, modifier: Modifier = Modifier) {
    val colors = Arcade.colors
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EthaLive(size = 150.dp, first = EthaMove.WAVE)
        Spacer(Modifier.height(16.dp))
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, color = colors.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = colors.muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        LedgeButton("Sign in", LocalSignInGate.current ?: {}, Modifier.fillMaxWidth(), icon = ArcadeIcons.Bolt)
    }
}
