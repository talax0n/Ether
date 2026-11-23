package dev.ether.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.ether.app.R
import dev.ether.app.ui.theme.Arcade
import kotlinx.coroutines.delay

/**
 * One row of res/drawable-nodpi/etha_sheet.png per move, frames left to right in square cells.
 * Copied from docs/brand/etha/manifest.json; EthaManifestTest fails if the two disagree.
 */
enum class EthaMove(val manifestKey: String, val row: Int, val frames: Int, val frameMs: Long, val loop: Boolean) {
    IDLE("idle", 0, 12, 83, true),
    WAVE("wave", 1, 11, 68, false),
    CELEBRATE("celebrate", 2, 11, 68, false),
    LEVEL_UP("levelup", 3, 6, 125, false),
    SLEEP("sleep", 4, 6, 250, true),
    LOOK("look", 5, 8, 100, false),
    SCRATCH("scratch", 6, 8, 100, false),
    TAP("tap", 7, 8, 100, false),
    REACT("react", 8, 8, 80, false),
    SHRUG("shrug", 9, 7, 100, false),
    HOP("hop", 10, 7, 70, false),
    RUN("run", 11, 8, 80, true),
    RUN_RIGHT("run_right", 12, 8, 80, true),
    RUN_LEFT("run_left", 13, 8, 80, true),
    ;

    companion object {
        /** Small things Etha does on its own between bigger moves, so idle never reads as a still. */
        val FIDGETS = listOf(LOOK, SCRATCH, TAP)

        /** What an awake Etha does unprompted: fidgets, plus the odd wave and hop. */
        val AMBIENT = FIDGETS + WAVE + HOP
    }
}

/** Cell edge in the sheet, in pixels. */
const val ETHA_CELL = 256

/**
 * Etha playing [move]. Loops run forever; one-shots stop on their last frame and call [onFinished].
 * Under reduced motion it shows a single still: the first frame of a loop, the last of a one-shot.
 */
@Composable
fun Etha(move: EthaMove, modifier: Modifier = Modifier, size: Dp = 120.dp, description: String? = null, onFinished: () -> Unit = {}) {
    val sheet = ImageBitmap.imageResource(R.drawable.etha_sheet)
    val reduced = Arcade.reducedMotion
    val finished by rememberUpdatedState(onFinished)
    var frame by remember(move) { mutableIntStateOf(if (reduced && !move.loop) move.frames - 1 else 0) }
    LaunchedEffect(move, reduced) {
        if (reduced) return@LaunchedEffect finished()
        while (true) {
            delay(move.frameMs)
            if (frame == move.frames - 1 && !move.loop) return@LaunchedEffect finished()
            frame = (frame + 1) % move.frames
        }
    }
    Canvas(modifier.size(size).semantics { description?.let { contentDescription = it } }) {
        val edge = this.size.minDimension.toInt()
        drawImage(
            sheet,
            srcOffset = IntOffset(frame * ETHA_CELL, move.row * ETHA_CELL),
            srcSize = IntSize(ETHA_CELL, ETHA_CELL),
            dstSize = IntSize(edge, edge),
            filterQuality = FilterQuality.High,
        )
    }
}

/**
 * Drives one on-screen Etha. [perform] plays a move and returns when it ends; a newer call interrupts
 * the one in progress, so a poke or a swipe reaction always wins over an ambient fidget.
 */
@Stable
class EthaController(val base: EthaMove = EthaMove.IDLE) {
    var move by mutableStateOf(base)
        private set

    /** Bumps on every play so the same move twice in a row restarts from frame 0. */
    var take by mutableIntStateOf(0)
        private set

    private val mutex = MutatorMutex()
    private val done = Channel<Unit>(Channel.CONFLATED)

    suspend fun perform(next: EthaMove) = mutex.mutate {
        done.tryReceive()
        move = next
        take++
        if (!next.loop) {
            done.receive()
            move = base
        }
    }

    /** Fire-and-forget [perform] that gives way quietly when something newer interrupts it. */
    fun play(scope: CoroutineScope, next: EthaMove) {
        scope.launch { performOrYield(next) }
    }

    internal fun finished() {
        done.trySend(Unit)
    }
}

/** [EthaController.perform], but an interruption by a newer move returns false instead of cancelling the caller. */
suspend fun EthaController.performOrYield(next: EthaMove): Boolean = try {
    perform(next)
    true
} catch (e: CancellationException) {
    currentCoroutineContext().ensureActive()
    false
}

@Composable
fun rememberEthaController(base: EthaMove = EthaMove.IDLE) = remember(base) { EthaController(base) }

/**
 * An Etha that is never still. It opens with [first], then every [restMs] plays something from [pool], and between
 * moves its whole body gently bobs and sways ([float]); a hop really leaves the ground. Tapping it plays
 * [EthaMove.REACT]. [ready] holds everything back until, for example, its container has animated in.
 * Under reduced motion it only reacts to taps and [first].
 */
@Composable
fun EthaLive(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    controller: EthaController = rememberEthaController(),
    first: EthaMove? = null,
    pool: List<EthaMove> = EthaMove.AMBIENT,
    restMs: LongRange = 1_500L..3_000L,
    float: Boolean = true,
    ready: suspend () -> Unit = {},
) {
    val reduced = Arcade.reducedMotion
    val scope = rememberCoroutineScope()
    LaunchedEffect(controller, reduced) {
        ready()
        first?.let { controller.performOrYield(it) }
        if (reduced || pool.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(restMs.first, restMs.last + 1))
            controller.performOrYield(pool.random())
        }
    }
    val moving = float && !reduced
    val asleep = controller.base == EthaMove.SLEEP
    val phase = if (moving) {
        rememberInfiniteTransition(label = "etha-float").animateFloat(
            0f, TAU, infiniteRepeatable(tween(if (asleep) 5_000 else 3_400, easing = LinearEasing)), label = "bob",
        )
    } else null
    val jump = remember { Animatable(0f) }
    LaunchedEffect(controller.take) {
        if (!moving || controller.move != EthaMove.HOP) return@LaunchedEffect
        val hop = EthaMove.HOP
        // The crouch frame stays grounded; the body rises and falls across the airborne frames.
        delay(hop.frameMs)
        val air = ((hop.frames - 2) * hop.frameMs).toInt()
        jump.animateTo(1f, tween(air / 2, easing = FastOutSlowInEasing))
        jump.animateTo(0f, tween(air / 2, easing = LinearOutSlowInEasing))
    }
    key(controller.take) {
        Etha(
            controller.move,
            modifier
                .graphicsLayer {
                    val p = phase?.value ?: return@graphicsLayer
                    val unit = size.toPx()
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    translationY = sin(p) * unit * 0.025f - jump.value * unit * 0.22f
                    if (!asleep) rotationZ = sin(p * 2f) * 2.5f
                }
                .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = "Poke Etha") {
                    controller.play(scope, EthaMove.REACT)
                },
            size,
            onFinished = controller::finished,
        )
    }
}

private const val TAU = (2 * PI).toFloat()
