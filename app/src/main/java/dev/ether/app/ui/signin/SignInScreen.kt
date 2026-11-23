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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import dev.ether.app.ui.onboarding.OnboardingScreen
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.theme.Motion
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.SunSoft
import dev.ether.app.ui.components.EthaLive
import dev.ether.app.ui.components.EthaMove
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import dev.ether.app.R
import dev.ether.app.ui.components.pressableLedge
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import dev.ether.app.BuildConfig
import kotlinx.coroutines.launch
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
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LedgeIconButton
import dev.ether.app.ui.components.ledge
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.EtherTheme
import dev.ether.app.ui.theme.NumberStyle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInScreen(seenOnboarding: Boolean, onOnboarded: () -> Unit, vm: SignInViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    // The demo backend has no Google project behind it, so its button signs straight in.
    val onGoogle: (() -> Unit)? = when {
        clientId.isNotBlank() -> { ->
            scope.launch {
                when (val r = pickGoogleAccount(context, clientId)) {
                    is GoogleResult.Token -> vm.signInWithGoogle(r.idToken, r.rawNonce)
                    GoogleResult.Cancelled -> Unit
                    is GoogleResult.Failed -> vm.googleFailed(r.message)
                }
            }
            Unit
        }
        BuildConfig.USE_FAKE_BACKEND -> { -> vm.signInWithGoogle("demo-id-token", "demo-nonce") }
        else -> null
    }
    // A way in picked on the welcome screen, held while the intro pages play the first time.
    var pending by rememberSaveable { mutableStateOf<WayIn?>(null) }
    val go = { way: WayIn ->
        when (way) {
            WayIn.EMAIL -> vm.continueWithEmail()
            WayIn.GOOGLE -> onGoogle?.invoke()
        }
    }
    val choose = { way: WayIn -> if (seenOnboarding) go(way) else pending = way }
    AnimatedContent(pending != null && !seenOnboarding, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "intro") { intro ->
        if (intro) {
            BackHandler { pending = null }
            OnboardingScreen(onDone = {
                val way = pending
                pending = null
                onOnboarded()
                way?.let(go)
            })
        } else {
            SignInContent(
                state, vm::onEmailChange, vm::onPasswordChange, vm::onModeChange, vm::onCodeChange, vm::submit, vm::back,
                vm::resend, vm::forgotPassword, { choose(WayIn.EMAIL) }, onGoogle?.let { { choose(WayIn.GOOGLE) } },
            )
        }
    }
}

private enum class WayIn { EMAIL, GOOGLE }

@Composable
fun SignInContent(
    state: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onModeChange: (SignInMode) -> Unit,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onResend: () -> Unit,
    onForgotPassword: () -> Unit,
    onContinueWithEmail: () -> Unit,
    /** Null hides the Google button: no client ID is configured for this build. */
    onGoogle: (() -> Unit)?,
) {
    val colors = Arcade.colors
    BackHandler(enabled = state.step != SignInStep.WELCOME && !state.busy, onBack = onBack)
    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().imePadding()) {
        if (state.step != SignInStep.WELCOME) {
            Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                LedgeIconButton(ArcadeIcons.Back, "Back", onBack, size = 42.dp)
            }
        }
        AnimatedContent(
            state.step,
            Modifier.weight(1f),
            transitionSpec = {
                // Deeper steps slide in from the right; going back slides the other way.
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally { dir * it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -dir * it / 4 } + fadeOut())
            },
            label = "step",
        ) { step ->
            if (step == SignInStep.WELCOME) {
                Welcome(onGoogle, onContinueWithEmail, busy = state.busy, error = state.error)
                return@AnimatedContent
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp)) {
                when (step) {
                    SignInStep.WELCOME -> Unit
                    SignInStep.CREDENTIALS -> {
                        EthaLive(Modifier.staggerIn(0).padding(bottom = 4.dp), size = 88.dp)
                        val creating = state.mode == SignInMode.CREATE_ACCOUNT
                        // The heading swaps with a short rise when the mode flips.
                        AnimatedContent(
                            creating, Modifier.staggerIn(1),
                            transitionSpec = { (fadeIn() + slideInVertically { it / 6 }) togetherWith fadeOut() },
                            label = "heading",
                        ) { isCreating ->
                            Column {
                                Heading(
                                    if (isCreating) "Create your\naccount" else "Welcome\nback",
                                    if (isCreating) "Use any email. We'll send a code to confirm it's yours." else "Sign in with your email and password.",
                                )
                            }
                        }
                        Box(Modifier.staggerIn(2)) { ModeSwitch(state.mode, onModeChange, enabled = !state.busy) }
                        Spacer(Modifier.height(16.dp))
                        ArcadeTextField(
                            value = state.email,
                            onValueChange = onEmailChange,
                            label = "Email",
                            modifier = Modifier.staggerIn(3),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.staggerIn(4)) { PasswordField(state.password, onPasswordChange, creating, onSubmit) }
                        if (creating) {
                            Spacer(Modifier.height(14.dp))
                        } else {
                            TextLink("Forgot password?", enabled = !state.busy, onForgotPassword, Modifier.align(Alignment.End).padding(vertical = 6.dp))
                        }
                        LedgeButton(
                            if (creating) "Create account" else "Sign in", onSubmit, Modifier.fillMaxWidth().staggerIn(5),
                            enabled = state.canSubmit || state.busy, busy = state.busy, icon = ArcadeIcons.Bolt,
                        )
                    }
                    SignInStep.VERIFY -> {
                        Heading("Verify your\nemail", "Enter the code we sent to ${state.email.trim()} to finish creating your account.")
                        CodeCells(state.code, onCodeChange, enabled = !state.busy)
                        Spacer(Modifier.height(16.dp))
                        ResendRow(state.resendIn, enabled = !state.busy, onResend)
                    }
                    SignInStep.RESET -> {
                        Heading("Reset your\npassword", "Enter the code we sent to ${state.email.trim()}, then choose a new password.")
                        CodeCells(state.code, onCodeChange, enabled = !state.busy)
                        Spacer(Modifier.height(16.dp))
                        ResendRow(state.resendIn, enabled = !state.busy, onResend)
                        Spacer(Modifier.height(20.dp))
                        PasswordField(state.password, onPasswordChange, creating = true, onSubmit, label = "New password (6+ characters)")
                    }
                }
                state.error?.let {
                    Spacer(Modifier.height(14.dp))
                    ErrorText(it)
                }
            }
        }
        if (state.step == SignInStep.VERIFY || state.step == SignInStep.RESET) {
            LedgeButton(
                if (state.step == SignInStep.RESET) "Set new password" else "Verify", onSubmit, Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                enabled = state.canSubmit || state.busy, busy = state.busy, icon = ArcadeIcons.Check,
            )
        }
    }
}

/** Two-way switch between signing in and creating an account; a Sun sticker slides under the selected side. */
@Composable
private fun ModeSwitch(mode: SignInMode, onChange: (SignInMode) -> Unit, enabled: Boolean) {
    val colors = Arcade.colors
    val shape = RoundedCornerShape(16.dp)
    val options = listOf(SignInMode.SIGN_IN to "Sign in", SignInMode.CREATE_ACCOUNT to "Create account")
    val slide by animateFloatAsState(if (mode == SignInMode.SIGN_IN) 0f else 1f, Motion.settle(), label = "mode")
    BoxWithConstraints(Modifier.fillMaxWidth().ledge(shape, colors.surface, colors.line, colors.ledge, 3.dp).padding(4.dp)) {
        val half = (maxWidth - 4.dp) / 2
        Box(
            Modifier.offset { IntOffset(((half + 4.dp) * slide).roundToPx(), 0) }
                .size(half, 44.dp).background(SunSoft, RoundedCornerShape(12.dp)),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (m, label) ->
                val selected = m == mode
                Box(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp))
                        .selectable(selected, enabled = enabled, role = Role.Tab) { onChange(m) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.titleSmall, color = if (selected) Ink else colors.muted)
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onChange: (String) -> Unit,
    creating: Boolean,
    onDone: () -> Unit,
    label: String = if (creating) "Password (6+ characters)" else "Password",
) {
    var shown by remember { mutableStateOf(false) }
    ArcadeTextField(
        value = value,
        onValueChange = onChange,
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onDone() }),
        visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton({ shown = !shown }) {
                Icon(if (shown) ArcadeIcons.EyeOff else ArcadeIcons.Eye, if (shown) "Hide password" else "Show password", tint = Arcade.colors.muted)
            }
        },
    )
}

/** Google's sign-in guidelines ask for the multicolor G, so this can't use LedgeButton's single-tint icon. */
@Composable
internal fun GoogleButton(onClick: () -> Unit, enabled: Boolean, shape: Shape = MaterialTheme.shapes.medium) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 61.dp)
            .pressableLedge(shape, Arcade.colors.surface, 5.dp, onClick, enabled),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Continue with Google", style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = Arcade.colors.text)
        }
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
private fun TextLink(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 4.dp, horizontal = 2.dp),
        style = MaterialTheme.typography.titleSmall,
        color = Arcade.colors.text,
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
        TextLink("Resend code", enabled, onResend)
    }
}

@Preview
@Composable
private fun SignInPreview() {
    EtherTheme { SignInContent(SignInUiState(email = "ana@example.com"), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) }
}
