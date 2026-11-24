package dev.ether.app.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.ether.app.R
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.EtherTheme
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

enum class OnboardingPage(val title: String, @DrawableRes val art: Int) {
    SWIPE("Swipe to find\nyour hackathon team", R.drawable.onboarding_swipe),
    MATCH("Match, then\nmessage", R.drawable.onboarding_match),
    LEVEL("Level up with\nevery event", R.drawable.onboarding_level),
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = OnboardingPage.entries
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    val colors = Arcade.colors

    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            if (!last) {
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onDone).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Skip", style = MaterialTheme.typography.titleSmall, color = colors.muted)
                    Icon(ArcadeIcons.Back, null, tint = colors.muted, modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = 180f })
                }
            }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { index ->
            val page = pages[index]
            val offset = pager.offsetOf(index)
            Column(Modifier.fillMaxSize()) {
                Image(
                    painterResource(page.art), contentDescription = null,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp).graphicsLayer {
                        // The art drifts slower than the page and fades, so pages feel layered rather than slid.
                        translationX = offset * size.width * 0.45f
                        alpha = 1f - offset.absoluteValue.coerceAtMost(1f) * 0.8f
                    },
                )
                Column(Modifier.padding(horizontal = 24.dp).graphicsLayer { alpha = 1f - offset.absoluteValue.coerceAtMost(1f) }) {
                    Text(page.title, style = MaterialTheme.typography.displaySmall, color = colors.text)
                }
            }
        }
        PageDots(pages.size, pager.currentPage, Modifier.padding(start = 24.dp, top = 22.dp))
        LedgeButton(
            if (last) "Get started" else "Continue",
            { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            icon = if (last) ArcadeIcons.Bolt else null,
            height = 61.dp,
            shape = RoundedCornerShape(50),
        )
    }
}

private fun PagerState.offsetOf(page: Int) = (currentPage - page) + currentPageOffsetFraction

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics { contentDescription = "Page ${current + 1} of $count" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val active = i == current
            val width by animateDpAsState(if (active) 22.dp else 8.dp, label = "dot-width")
            val fill by animateColorAsState(if (active) Coral else Arcade.colors.track, label = "dot-fill")
            Box(Modifier.width(width).height(8.dp).background(fill, CircleShape))
        }
    }
}


@Preview
@Composable
private fun OnboardingPreview() {
    EtherTheme { OnboardingScreen {} }
}
