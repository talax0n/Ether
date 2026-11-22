package dev.ether.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.first

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
 * Etha plays the one-shot [move], idles for [restMs], and plays it again. Idle alone is a soft breathe
 * that reads as a still, so a screen that should feel alive repeats its move. [ready] holds the first
 * play back, e.g. until a container has animated in. Under reduced motion the move plays once.
 */
@Composable
fun EthaRepeating(move: EthaMove, restMs: Long, modifier: Modifier = Modifier, size: Dp = 120.dp, ready: suspend () -> Unit = {}) {
    var current by remember { mutableStateOf(EthaMove.IDLE) }
    val reduced = Arcade.reducedMotion
    LaunchedEffect(move, reduced) {
        ready()
        do {
            current = move
            snapshotFlow { current }.first { it == EthaMove.IDLE }
            delay(restMs)
        } while (!reduced)
    }
    Etha(current, modifier, size, onFinished = { if (current == move) current = EthaMove.IDLE })
}
