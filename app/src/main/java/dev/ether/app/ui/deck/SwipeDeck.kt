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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ether.app.domain.DeckCard
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.domain.levelFor
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.Avatar
import dev.ether.app.ui.components.CapsLabel
import dev.ether.app.ui.components.Chip
import dev.ether.app.ui.components.LevelHex
import dev.ether.app.ui.components.LineWidth
import dev.ether.app.ui.components.TagRow
import dev.ether.app.ui.components.ledge
import dev.ether.app.ui.components.pressableLedge
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.CoralDeep
import dev.ether.app.ui.theme.CoralSoft
import dev.ether.app.ui.theme.GrapeSoft
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.Mint
import dev.ether.app.ui.theme.MintDeep
import dev.ether.app.ui.theme.MintSoft
import dev.ether.app.ui.theme.Peach
import dev.ether.app.ui.theme.Sky
import dev.ether.app.ui.theme.SkySoft
import dev.ether.app.ui.theme.SunSoft
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** Fraction of the card width a drag must pass to commit on release. */
private const val COMMIT_FRACTION = 0.35f
private const val MAX_ROTATION_DEG = 14f
private val FlingVelocity = 1100.dp
private const val NEXT_CARD_SCALE = 0.94f
private val NEXT_CARD_DROP = 16.dp

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
fun SwipeDeck(
    cards: List<DeckCard>,
    onSwipe: (DeckCard, SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
    /** False while the previous swipe is still on its way to the database. */
    enabled: Boolean = true,
) {
    val top = cards.firstOrNull() ?: return
    val next = cards.getOrNull(1)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val currentEnabled by rememberUpdatedState(enabled)
    val state = remember(top.listingId) { SwipeCardState() }

    fun commit(direction: SwipeDirection) {
        if (state.committing || !currentEnabled) return
        state.committing = true
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        scope.launch {
            state.flyAway(direction)
            currentOnSwipe(top, direction)
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (next != null) key(next.listingId) {
                CardFace(
                    next,
                    stampProgress = { 0f },
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val p = abs(state.progress)
                        val scale = NEXT_CARD_SCALE + (1f - NEXT_CARD_SCALE) * p
                        scaleX = scale
                        scaleY = scale
                        translationY = (1f - p) * NEXT_CARD_DROP.toPx()
                    },
                )
            }
            key(top.listingId) {
                CardFace(
                    top,
                    stampProgress = { state.progress },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("top_card")
                        .onSizeChanged { state.width = it.width.toFloat() }
                        // Gesture detection sits outside graphicsLayer so pointer positions stay in the
                        // unmoved frame; otherwise the finger looks stationary to the velocity tracker.
                        .pointerInput(top.listingId) {
                            val flingPx = FlingVelocity.toPx()
                            coroutineScope {
                                val tracker = VelocityTracker()
                                detectDragGestures(
                                    onDragStart = { tracker.resetTracking() },
                                    // Once committing, flyAway owns the offset: a springBack here would cancel it
                                    // before onSwipe runs and leave the card stuck.
                                    onDragEnd = {
                                        if (state.committing) return@detectDragGestures
                                        val x = state.offset.value.x
                                        val vx = tracker.calculateVelocity().x
                                        val passed = abs(x) > state.width * COMMIT_FRACTION
                                        val flung = abs(vx) > flingPx && sign(vx) == sign(x)
                                        when {
                                            (passed || flung) && currentEnabled -> commit(if (x > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT)
                                            else -> launch { state.springBack() }
                                        }
                                    },
                                    onDragCancel = { if (!state.committing) launch { state.springBack() } },
                                ) { change, drag ->
                                    if (state.committing || !currentEnabled) return@detectDragGestures
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
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Each button swells as the card is dragged towards it; read in the draw phase.
            RoundAction(
                ArcadeIcons.Close, "Pass on ${top.name}", { commit(SwipeDirection.LEFT) }, enabled,
                size = 72.dp, color = Arcade.colors.surface, tint = CoralDeep, swell = { (-state.progress).coerceAtLeast(0f) },
                modifier = Modifier.testTag("pass"),
            )
            RoundAction(
                ArcadeIcons.HeartFilled, "Like ${top.name}", { commit(SwipeDirection.RIGHT) }, enabled,
                size = 84.dp, color = Mint, tint = Ink, swell = { state.progress.coerceAtLeast(0f) },
                modifier = Modifier.testTag("like"),
            )
        }
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    size: Dp,
    color: Color,
    tint: Color,
    swell: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .graphicsLayer {
                val s = 1f + 0.12f * swell()
                scaleX = s
                scaleY = s
            }
            .size(size + 5.dp)
            .pressableLedge(CircleShape, color, 5.dp, onClick, enabled)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.42f)) }
}

private val HeaderHeight = 156.dp
private val AvatarSize = 104.dp

@Composable
private fun CardFace(card: DeckCard, stampProgress: () -> Float, modifier: Modifier = Modifier) {
    val team = card is DeckCard.Team
    val xp = when (card) {
        is DeckCard.Solo -> card.xp
        is DeckCard.Team -> card.xp
    }
    Box(modifier.ledge(MaterialTheme.shapes.large, Arcade.colors.surface, 6.dp)) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(HeaderHeight)
                    .background(if (team) Sky else Coral)
                    .drawBehind {
                        // Halftone dots, the mockup's radial-gradient pattern.
                        val step = 18.dp.toPx()
                        val r = 2.5.dp.toPx()
                        val dot = Color.White.copy(alpha = 0.28f)
                        var y = step / 2
                        while (y < size.height) {
                            var x = step / 2
                            while (x < size.width) {
                                drawCircle(dot, r, Offset(x, y))
                                x += step
                            }
                            y += step
                        }
                        drawRect(Ink, Offset(0f, size.height - LineWidth.toPx()), androidx.compose.ui.geometry.Size(size.width, LineWidth.toPx()))
                    },
            ) {
                Chip(if (team) "TEAM" else "SOLO", Modifier.align(Alignment.TopStart).padding(16.dp), color = if (team) GrapeSoft else SunSoft)
                LevelHex(levelFor(xp).number, Modifier.align(Alignment.TopEnd).padding(14.dp), width = 56.dp, caption = true)
            }
            Column(
                Modifier.padding(start = 22.dp, end = 22.dp, top = AvatarSize / 2 + 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (card) {
                    is DeckCard.Solo -> SoloBody(card)
                    is DeckCard.Team -> TeamBody(card)
                }
            }
        }
        Avatar(
            card.name, card.avatarUrl, AvatarSize,
            Modifier.offset(x = 22.dp, y = HeaderHeight - AvatarSize / 2 - 4.dp),
            color = if (team) SkySoft else Peach,
        )
        Stamp("LIKE", MintDeep, -14f, Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 62.dp)) { stampProgress().coerceAtLeast(0f) }
        Stamp("NOPE", CoralDeep, 14f, Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 62.dp)) { (-stampProgress()).coerceAtLeast(0f) }
    }
}

@Composable
private fun Name(text: String) {
    Text(text, style = MaterialTheme.typography.headlineLarge, color = Arcade.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Stats(xp: Int, reliability: String, extra: String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), maxLines = 1) {
        Chip(reliability, color = MintSoft, icon = ArcadeIcons.Shield)
        Chip("$xp XP", color = SunSoft, numeric = true)
        Chip(extra, color = GrapeSoft)
    }
}

@Composable
private fun SoloBody(card: DeckCard.Solo) {
    Name(card.name)
    Stats(card.xp, card.reliability.label, levelFor(card.xp).title)
    Text(card.headline, style = MaterialTheme.typography.bodyLarge, color = Arcade.colors.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
    CapsLabel("Skills")
    TagRow(card.skills, SkySoft, maxLines = 1)
    CapsLabel("Roles")
    TagRow(card.roles, CoralSoft, maxLines = 1)
}

@Composable
private fun TeamBody(card: DeckCard.Team) {
    Name(card.name)
    Stats(card.xp, card.reliability.label, if (card.memberCount == 1) "1 member" else "${card.memberCount} members")
    Text(card.pitch, style = MaterialTheme.typography.bodyLarge, color = Arcade.colors.text, maxLines = 4, overflow = TextOverflow.Ellipsis)
    CapsLabel("Looking for")
    TagRow(card.openRoles, CoralSoft, maxLines = 2)
}

@Composable
private fun Stamp(text: String, color: Color, rotation: Float, modifier: Modifier, alpha: () -> Float) {
    val shape = RoundedCornerShape(14.dp)
    Text(
        text,
        color = color,
        fontSize = 40.sp,
        letterSpacing = 2.sp,
        style = MaterialTheme.typography.displayMedium,
        modifier = modifier
            .graphicsLayer {
                val a = alpha()
                this.alpha = a
                rotationZ = rotation
                // Lands like a rubber stamp: starts big, settles to 1 as the drag nears the commit line.
                val s = 1.4f - 0.4f * a
                scaleX = s
                scaleY = s
            }
            .background(Color.White.copy(alpha = 0.85f), shape)
            .border(5.dp, color, shape)
            .padding(horizontal = 14.dp, vertical = 0.dp),
    )
}
