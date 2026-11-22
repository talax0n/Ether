package dev.ether.app.ui.signin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.ArcadeTextField
import dev.ether.app.ui.components.ErrorText
import dev.ether.app.ui.components.EtherMark
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LedgeIconButton
import dev.ether.app.ui.components.ledge
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.EtherTheme
import dev.ether.app.ui.theme.NumberStyle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInScreen(vm: SignInViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    SignInContent(state, vm::onEmailChange, vm::onCodeChange, vm::submit, vm::changeEmail, vm::resend)
}

@Composable
fun SignInContent(
    state: SignInUiState,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onChangeEmail: () -> Unit,
    onResend: () -> Unit,
) {
    val colors = Arcade.colors
    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.step == SignInStep.CODE) {
                LedgeIconButton(ArcadeIcons.Back, "Use a different email", onChangeEmail, size = 42.dp)
            } else {
                EtherMark(size = 40.dp)
            }
        }
        AnimatedContent(
            state.step,
            Modifier.weight(1f),
            transitionSpec = {
                val dir = if (targetState == SignInStep.CODE) 1 else -1
                (slideInHorizontally { dir * it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -dir * it / 4 } + fadeOut())
            },
            label = "step",
        ) { step ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp)) {
                when (step) {
                    SignInStep.EMAIL -> {
                        Heading("Sign in or\ncreate an account", "Enter your email and we'll send a 6-digit code. New here? The code creates your account.")
                        ArcadeTextField(
                            value = state.email,
                            onValueChange = onEmailChange,
                            label = "Email",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                        )
                    }
                    SignInStep.CODE -> {
                        Heading("Check your\ninbox", "Enter the code we sent to ${state.email.trim()}.")
                        CodeCells(state.code, onCodeChange, enabled = !state.busy)
                        Spacer(Modifier.height(16.dp))
                        ResendRow(state.resendIn, enabled = !state.busy, onResend)
                    }
                }
                state.error?.let {
                    Spacer(Modifier.height(14.dp))
                    ErrorText(it)
                }
            }
        }
        LedgeButton(
            if (state.step == SignInStep.EMAIL) "Send code" else "Verify",
            onSubmit,
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            enabled = state.canSubmit || state.busy,
            busy = state.busy,
            icon = if (state.step == SignInStep.EMAIL) ArcadeIcons.Bolt else ArcadeIcons.Check,
        )
    }
}

@Composable
private fun Heading(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.displaySmall, color = Arcade.colors.text)
    Spacer(Modifier.height(10.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = Arcade.colors.muted)
    Spacer(Modifier.height(28.dp))
}

/** Six code boxes over one hidden text field, so paste, autofill and the keyboard all behave like a normal input. */
@Composable
private fun CodeCells(code: String, onChange: (String) -> Unit, enabled: Boolean) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val colors = Arcade.colors
    BasicTextField(
        value = code,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "6-digit code" },
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(SignInUiState.CODE_LENGTH) { i ->
                    val active = enabled && i == code.length
                    Box(
                        Modifier.weight(1f).height(64.dp)
                            .ledge(RoundedCornerShape(14.dp), colors.surface, if (active) Coral else colors.line, colors.ledge, if (active) 5.dp else 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(code.getOrNull(i)?.toString() ?: "", style = NumberStyle.copy(fontSize = 26.sp), color = colors.text)
                    }
                }
            }
        },
    )
}

@Composable
private fun ResendRow(resendIn: Int, enabled: Boolean, onResend: () -> Unit) {
    val colors = Arcade.colors
    if (resendIn > 0) {
        Text(
            "Resend code in 0:${resendIn.toString().padStart(2, '0')}",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
    } else {
        Text(
            "Resend code",
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onResend).padding(vertical = 4.dp),
            style = MaterialTheme.typography.titleSmall,
            color = colors.text,
        )
    }
}

@Preview
@Composable
private fun SignInPreview() {
    EtherTheme { SignInContent(SignInUiState(email = "ana@student.ac.id"), {}, {}, {}, {}, {}) }
}
