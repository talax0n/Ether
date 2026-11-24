package dev.ether.app.ui.signin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.SunSoft
import dev.ether.app.ui.components.EthaLive
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
import dev.ether.app.ui.theme.EtherTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
/** [startMode] opens straight at the email form in that mode; null starts on the Google-or-email welcome. */
fun SignInScreen(onClose: () -> Unit, startMode: SignInMode? = null, vm: SignInViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val startAtEmail = startMode != null
    // The ViewModel outlives this overlay, so set the starting step each time it opens.
    LaunchedEffect(Unit) {
        if (startMode == null) vm.back()
        else {
            vm.onModeChange(startMode)
            vm.continueWithEmail()
        }
    }
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
    SignInContent(
        state, vm::onEmailChange, vm::onPasswordChange, vm::onModeChange, vm::submit,
        // Opened straight at email (e.g. "I already have an account"), back from there closes instead.
        { if (startAtEmail && state.step == SignInStep.CREDENTIALS) onClose() else vm.back() },
        vm::continueWithEmail, onGoogle, onClose,
    )
}

@Composable
fun SignInContent(
    state: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onModeChange: (SignInMode) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onContinueWithEmail: () -> Unit,
    /** Null hides the Google button: no client ID is configured for this build. */
    onGoogle: (() -> Unit)?,
    /** Leaves sign-in and goes back to browsing as a guest. */
    onClose: () -> Unit,
) {
    val colors = Arcade.colors
    BackHandler(enabled = !state.busy) { if (state.step == SignInStep.WELCOME) onClose() else onBack() }
    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().imePadding()) {
      // Etha is one shared element across steps: leaving the welcome screen it flies from the burst's centre
      // into its spot above the form, while the bubbles scatter outward and the form rises in.
      SharedTransitionLayout(Modifier.weight(1f)) {
        AnimatedContent(
            state.step,
            Modifier.fillMaxSize(),
            transitionSpec = {
                when {
                    initialState == SignInStep.WELCOME ->
                        (fadeIn(tween(320, delayMillis = 140)) + slideInVertically(tween(420, delayMillis = 100)) { it / 10 }) togetherWith
                            fadeOut(tween(360))
                    targetState == SignInStep.WELCOME ->
                        fadeIn(tween(360, delayMillis = 80)) togetherWith
                            (fadeOut(tween(200)) + slideOutVertically(tween(260)) { it / 10 })
                    else -> {
                        // Deeper steps slide in from the right; going back slides the other way.
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally { dir * it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -dir * it / 4 } + fadeOut())
                    }
                }
            },
            label = "step",
        ) { step ->
            val ethaShared = Modifier.sharedElement(
                rememberSharedContentState("etha"), this@AnimatedContent,
                boundsTransform = { _, _ -> spring(dampingRatio = 0.75f, stiffness = 260f) },
            )
            if (step == SignInStep.WELCOME) {
                // 0 while shown, 1 once gone: drives the bubbles' scatter out and back in.
                val scatter by transition.animateFloat({ tween(420) }, label = "scatter") { if (it == EnterExitState.Visible) 0f else 1f }
                Welcome(
                    actions = {
                        onGoogle?.let { GoogleButton(it, enabled = !state.busy, shape = WelcomePill) }
                        LedgeButton(
                            "Continue with email", onContinueWithEmail, Modifier.fillMaxWidth(),
                            enabled = !state.busy, icon = ArcadeIcons.Bolt, height = 61.dp, shape = WelcomePill,
                        )
                    },
                    onClose = onClose, error = state.error, ethaModifier = ethaShared, scatter = { scatter },
                )
                return@AnimatedContent
            }
            Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                LedgeIconButton(ArcadeIcons.Back, "Back", onBack, size = 42.dp)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp)) {
                when (step) {
                    SignInStep.WELCOME -> Unit
                    SignInStep.CREDENTIALS -> {
                        EthaLive(ethaShared.padding(bottom = 4.dp), size = 88.dp)
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
                                    if (isCreating) "Use any email and pick a password." else "Sign in with your email and password.",
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
                        Spacer(Modifier.height(14.dp))
                        LedgeButton(
                            if (creating) "Create account" else "Sign in", onSubmit, Modifier.fillMaxWidth().staggerIn(5),
                            enabled = state.canSubmit || state.busy, busy = state.busy, icon = ArcadeIcons.Bolt,
                        )
                    }
                }
                state.notice?.let {
                    Spacer(Modifier.height(14.dp))
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.text)
                }
                state.error?.let {
                    Spacer(Modifier.height(14.dp))
                    ErrorText(it)
                }
            }
            }
        }
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


@Preview
@Composable
private fun SignInPreview() {
    EtherTheme { SignInContent(SignInUiState(email = "ana@example.com"), {}, {}, {}, {}, {}, {}, {}, {}) }
}
