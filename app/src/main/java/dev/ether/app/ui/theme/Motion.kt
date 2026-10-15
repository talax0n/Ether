package dev.ether.app.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/** Motion tokens. `pop` is the spec's spring(dampingRatio 0.5, stiffness Medium), CSS cubic-bezier(.34,1.56,.64,1). */
object Motion {
    const val PRESS_MS = 80
    const val STAGGER_MS = 70L
    const val REWARD_STAGGER_MS = 120L
    const val COUNT_UP_MS = 900
    const val RAYS_PERIOD_MS = 18_000
    const val SCREEN_MS = 320

    fun <T> pop(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
    fun <T> settle(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
    fun <T> press(): FiniteAnimationSpec<T> = tween(PRESS_MS)
}
