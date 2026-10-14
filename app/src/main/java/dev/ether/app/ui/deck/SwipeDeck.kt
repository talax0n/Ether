package dev.ether.app.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ether.app.R
import dev.ether.app.domain.DeckCard
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.ui.Avatar
import dev.ether.app.ui.TagRow
import dev.ether.app.ui.theme.LikeGreen
import dev.ether.app.ui.theme.NopeRed
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** Fraction of the card width a drag must pass to commit on release. */
private const val COMMIT_FRACTION = 0.35f
private const val MAX_ROTATION_DEG = 14f
private val FlingVelocity = 1100.dp
private const val NEXT_CARD_SCALE = 0.93f

private class SwipeCardState {
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)
    var width by mutableFloatStateOf(1f)
    var committing by mutableStateOf(false)

    /** -1..1, reaching ±1 exactly at the commit threshold. */
    val progress: Float get() = (offset.value.x / (width * COMMIT_FRACTION)).coerceIn(-1f, 1f)

    suspend fun flyAway(direction: SwipeDirection) {
        val sign = if (direction == SwipeDirection.RIGHT) 1f else -1f
        offset.animateTo(Offset(sign * width * 1.5f, offset.value.y + width * 0.1f), tween(durationMillis = 220))
    }

    suspend fun springBack() {
        offset.animateTo(Offset.Zero, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }
}

@Composable
fun SwipeDeck(cards: List<DeckCard>, onSwipe: (DeckCard, SwipeDirection) -> Unit, modifier: Modifier = Modifier) {
    val top = cards.firstOrNull() ?: return
    val next = cards.getOrNull(1)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val state = remember(top.listingId) { SwipeCardState() }

    fun commit(direction: SwipeDirection) {
        if (state.committing) return
        state.committing = true
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        scope.launch {
            state.flyAway(direction)
            currentOnSwipe(top, direction)
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (next != null) key(next.listingId) {
                CardFace(
                    next,
                    stampProgress = { 0f },
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val scale = NEXT_CARD_SCALE + (1f - NEXT_CARD_SCALE) * abs(state.progress)
                        scaleX = scale
                        scaleY = scale
                    },
                )
            }
            key(top.listingId) {
                CardFace(
                    top,
                    stampProgress = { state.progress },
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { state.width = it.width.toFloat() }
                        // Gesture detection sits outside graphicsLayer so pointer positions stay in the
                        // unmoved frame; otherwise the finger looks stationary to the velocity tracker.
                        .pointerInput(top.listingId) {
                            val flingPx = FlingVelocity.toPx()
                            coroutineScope {
                                val tracker = VelocityTracker()
                                detectDragGestures(
                                    onDragStart = { tracker.resetTracking() },
                                    onDragEnd = {
                                        val x = state.offset.value.x
                                        val vx = tracker.calculateVelocity().x
                                        val passed = abs(x) > state.width * COMMIT_FRACTION
                                        val flung = abs(vx) > flingPx && sign(vx) == sign(x)
                                        when {
                                            passed || flung -> commit(if (x > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT)
                                            else -> launch { state.springBack() }
                                        }
                                    },
                                    onDragCancel = { launch { state.springBack() } },
                                ) { change, drag ->
                                    if (state.committing) return@detectDragGestures
                                    change.consume()
                                    tracker.addPointerInputChange(change)
                                    launch { state.offset.snapTo(state.offset.value + drag) }
                                }
                            }
                        }
                        .graphicsLayer {
                            translationX = state.offset.value.x
                            translationY = state.offset.value.y
                            rotationZ = state.offset.value.x / state.width * MAX_ROTATION_DEG
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction("Like") { commit(SwipeDirection.RIGHT); true },
                                CustomAccessibilityAction("Pass") { commit(SwipeDirection.LEFT); true },
                            )
                        },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally)) {
            FilledIconButton(
                onClick = { commit(SwipeDirection.LEFT) },
                modifier = Modifier.size(68.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = NopeRed,
                ),
            ) { Icon(painterResource(R.drawable.ic_close), "Pass on ${top.name}", Modifier.size(32.dp)) }
            FilledIconButton(
                onClick = { commit(SwipeDirection.RIGHT) },
                modifier = Modifier.size(68.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = LikeGreen, contentColor = Color.White),
            ) { Icon(painterResource(R.drawable.ic_favorite), "Like ${top.name}", Modifier.size(32.dp)) }
        }
    }
}

@Composable
private fun CardFace(card: DeckCard, stampProgress: () -> Float, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                CardHeader(card)
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (card) {
                        is DeckCard.Solo -> SoloBody(card)
                        is DeckCard.Team -> TeamBody(card)
                    }
                }
            }
            Stamp("LIKE", LikeGreen, -16f, Modifier.align(Alignment.TopStart)) { stampProgress().coerceAtLeast(0f) }
            Stamp("NOPE", NopeRed, 16f, Modifier.align(Alignment.TopEnd)) { (-stampProgress()).coerceAtLeast(0f) }
        }
    }
}

@Composable
private fun CardHeader(card: DeckCard) {
    val scheme = MaterialTheme.colorScheme
    val brush = when (card) {
        is DeckCard.Solo -> Brush.linearGradient(listOf(scheme.primary, scheme.tertiary))
        is DeckCard.Team -> Brush.linearGradient(listOf(scheme.secondary, scheme.primary))
    }
    Box(Modifier.fillMaxWidth().height(170.dp).background(brush), contentAlignment = Alignment.Center) {
        Avatar(card.name, card.avatarUrl, 104.dp)
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.35f),
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
        ) {
            Text(
                if (card is DeckCard.Team) "TEAM" else "SOLO",
                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SoloBody(card: DeckCard.Solo) {
    Text(card.name, style = MaterialTheme.typography.headlineSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("${card.xp} XP")
        Pill(card.reliability.label)
    }
    Text(card.headline, maxLines = 4, overflow = TextOverflow.Ellipsis)
    TagRow(card.roles, container = MaterialTheme.colorScheme.tertiaryContainer)
    TagRow(card.skills)
}

@Composable
private fun TeamBody(card: DeckCard.Team) {
    Text(card.name, style = MaterialTheme.typography.headlineSmall)
    Text(if (card.memberCount == 1) "1 member" else "${card.memberCount} members", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("${card.xp} XP")
        Pill(card.reliability.label)
    }
    Text(card.pitch, maxLines = 5, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(4.dp))
    Text("Looking for", style = MaterialTheme.typography.titleSmall)
    TagRow(card.openRoles, container = MaterialTheme.colorScheme.tertiaryContainer)
}

@Composable
private fun Pill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Stamp(text: String, color: Color, rotation: Float, modifier: Modifier, alpha: () -> Float) {
    Text(
        text,
        color = color,
        fontSize = 40.sp,
        fontWeight = FontWeight.Black,
        modifier = modifier
            .padding(top = 36.dp, start = 24.dp, end = 24.dp)
            .graphicsLayer {
                this.alpha = alpha()
                rotationZ = rotation
            }
            .border(4.dp, color, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 2.dp),
    )
}
