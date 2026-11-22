package dev.ether.app.ui.components

import androidx.compose.foundation.Canvas
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
    ;

    companion object {
        /** Small things Etha does on its own between bigger moves, so idle never reads as a still. */
        val FIDGETS = listOf(LOOK, SCRATCH, TAP)
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
 * An Etha that is never still: it opens with [first], then every [restMs] plays something from [pool]
 * (fidgets by default). Tapping it plays [EthaMove.REACT]. [ready] holds everything back until, for
 * example, its container has animated in. Under reduced motion it only reacts to taps and [first].
 */
@Composable
fun EthaLive(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    controller: EthaController = rememberEthaController(),
    first: EthaMove? = null,
    pool: List<EthaMove> = EthaMove.FIDGETS,
    restMs: LongRange = 2_500L..5_000L,
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
    key(controller.take) {
        Etha(
            controller.move,
            modifier.clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = "Poke Etha") {
                controller.play(scope, EthaMove.REACT)
            },
            size,
            onFinished = controller::finished,
        )
    }
}
