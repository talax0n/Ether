package dev.ether.app.ui.signin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.ArcadeTextField
import dev.ether.app.ui.components.ErrorText
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LedgeCard
import dev.ether.app.ui.components.Wordmark
import dev.ether.app.ui.components.ledge
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Grape
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.Mint
import dev.ether.app.ui.theme.Sky
import dev.ether.app.ui.theme.Sun
import dev.ether.app.ui.theme.EtherTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.sin

@Composable
fun SignInScreen(vm: SignInViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    SignInContent(state, vm::onEmailChange, vm::onCodeChange, vm::submit, vm::changeEmail)
}

@Composable
fun SignInContent(
    state: SignInUiState,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onChangeEmail: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Arcade.colors.background)) {
        Column(
            Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Stickers(Modifier.staggerIn(0))
            Spacer(Modifier.height(18.dp))
            Wordmark(Modifier.staggerIn(1))
            Spacer(Modifier.height(4.dp))
            Text(
                "Swipe to find your hackathon team.",
                Modifier.staggerIn(2),
                style = MaterialTheme.typography.titleLarge,
                color = Arcade.colors.text,
            )
            Spacer(Modifier.height(28.dp))
            LedgeCard(Modifier.fillMaxWidth().staggerIn(3)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AnimatedContent(
                        state.step,
                        transitionSpec = {
                            val dir = if (targetState == SignInStep.CODE) 1 else -1
                            (slideInHorizontally { dir * it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -dir * it / 3 } + fadeOut())
                        },
                        label = "step",
                    ) { step ->
                        when (step) {
                            SignInStep.EMAIL -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Use your campus address. We'll email you a code.", style = MaterialTheme.typography.bodyMedium, color = Arcade.colors.muted)
                                ArcadeTextField(
                                    value = state.email,
                                    onValueChange = onEmailChange,
                                    label = "University email",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                                )
                            }
                            SignInStep.CODE -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("We sent a 6-digit code to ${state.email.trim()}.", style = MaterialTheme.typography.bodyMedium, color = Arcade.colors.muted)
                                ArcadeTextField(
                                    value = state.code,
                                    onValueChange = onCodeChange,
                                    label = "6-digit code",
                                    numeric = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                                )
                            }
                        }
                    }
                    state.error?.let { ErrorText(it) }
                    LedgeButton(
                        if (state.step == SignInStep.EMAIL) "Send code" else "Verify",
                        onSubmit,
                        Modifier.fillMaxWidth(),
                        enabled = state.canSubmit || state.busy,
                        busy = state.busy,
                        icon = if (state.step == SignInStep.EMAIL) ArcadeIcons.Bolt else ArcadeIcons.Check,
                    )
                }
            }
            if (state.step == SignInStep.CODE) {
                Spacer(Modifier.height(12.dp))
                LedgeButton(
                    "Use a different email", onChangeEmail, Modifier.fillMaxWidth(),
                    color = Color.Transparent, contentColor = Arcade.colors.text, height = 48.dp,
                )
            }
        }
    }
}

/** Four reward stickers that bob gently above the logo: the arcade promise before the first tap. */
@Composable
private fun Stickers(modifier: Modifier = Modifier) {
    val t = if (Arcade.reducedMotion) remember { mutableFloatStateOf(0f) } else rememberInfiniteTransition(label = "bob")
        .animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val items = listOf<Triple<ImageVector, Color, Float>>(
        Triple(ArcadeIcons.HeartFilled, Mint, -10f),
        Triple(ArcadeIcons.Bolt, Sun, 6f),
        Triple(ArcadeIcons.Trophy, Sky, -4f),
        Triple(ArcadeIcons.Users, Grape, 10f),
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { i, (icon, color, tilt) ->
            Box(
                Modifier
                    .graphicsLayer {
                        translationY = sin(t.value + i * 1.3f) * 4.dp.toPx()
                        rotationZ = tilt
                    }
                    .size(52.dp)
                    .ledge(CircleShape, color, Arcade.colors.line, Arcade.colors.ledge, 4.dp),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(24.dp)) }
        }
    }
}

@Preview
@Composable
private fun SignInPreview() {
    EtherTheme { SignInContent(SignInUiState(email = "ana@student.ac.id"), {}, {}, {}, {}) }
}
