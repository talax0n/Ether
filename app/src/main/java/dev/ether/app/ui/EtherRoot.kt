package dev.ether.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.MessageBox
import dev.ether.app.ui.components.SplashLoading
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.Motion
import dev.ether.app.ui.theme.SunSoft
import dev.ether.app.ui.competitions.CompetitionDetailScreen
import dev.ether.app.ui.competitions.CompetitionListScreen
import dev.ether.app.ui.deck.DeckScreen
import dev.ether.app.ui.matches.MatchesScreen
import dev.ether.app.ui.profile.ProfileEditScreen
import dev.ether.app.ui.profile.ProfileScreen
import dev.ether.app.ui.signin.SignInScreen
import dev.ether.app.ui.teams.TeamsScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import kotlin.reflect.KClass

@Serializable data object CompetitionsRoute
@Serializable data class CompetitionDetailRoute(val competitionId: String)
@Serializable data class DeckRoute(val competitionId: String, val listingId: String)
@Serializable data class MatchesRoute(val competitionId: String)
@Serializable data object AllMatchesRoute
@Serializable data object TeamsRoute
@Serializable data object ProfileRoute
@Serializable data object EditProfileRoute

private enum class Tab(val route: Any, val routeClass: KClass<*>, val label: String, val icon: ImageVector) {
    COMPETITIONS(CompetitionsRoute, CompetitionsRoute::class, "Compete", ArcadeIcons.Trophy),
    MATCHES(AllMatchesRoute, AllMatchesRoute::class, "Matches", ArcadeIcons.Heart),
    TEAMS(TeamsRoute, TeamsRoute::class, "Teams", ArcadeIcons.Users),
    PROFILE(ProfileRoute, ProfileRoute::class, "Profile", ArcadeIcons.User),
}

@Composable
fun EtherRoot(vm: AppViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        AppUiState.Loading -> SplashLoading()
        AppUiState.SignedOut -> SignInScreen()
        // Keyed by user: this screen lives outside the NavHost, so its ViewModel would otherwise outlive a sign-out.
        is AppUiState.NeedsProfile -> ProfileEditScreen(
            isSetup = true, onDone = vm::recheck, onBack = {},
            vm = koinViewModel(key = "profile-setup-${s.userId.value}"),
        )
        is AppUiState.Failed -> Box(Modifier.background(Arcade.colors.background)) {
            MessageBox(s.message, action = "Retry", tone = Tone.ERROR, title = "Can't load your profile", onAction = vm::recheck)
        }
        AppUiState.Ready -> MainNavHost()
    }
}

private fun NavHostController.navigateToTab(tab: Tab) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun MainNavHost() {
    val nav = rememberNavController()
    val destination = nav.currentBackStackEntryAsState().value?.destination
    val currentTab = Tab.entries.firstOrNull { tab -> destination?.hasRoute(tab.routeClass) == true }
    val reduced = Arcade.reducedMotion
    Scaffold(
        containerColor = Arcade.colors.background,
        bottomBar = {
            // Only on the four top-level screens; detail, deck and edit screens get the whole height.
            if (currentTab != null) ArcadeNavBar(currentTab) { nav.navigateToTab(it) }
        },
    ) { padding ->
        NavHost(
            nav, startDestination = CompetitionsRoute, modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { if (reduced) EnterTransition.None else fadeIn(tween(Motion.SCREEN_MS)) + slideInHorizontally(tween(Motion.SCREEN_MS)) { it / 6 } },
            exitTransition = { if (reduced) ExitTransition.None else fadeOut(tween(Motion.SCREEN_MS / 2)) },
            popEnterTransition = { if (reduced) EnterTransition.None else fadeIn(tween(Motion.SCREEN_MS)) },
            popExitTransition = { if (reduced) ExitTransition.None else fadeOut(tween(Motion.SCREEN_MS / 2)) + slideOutHorizontally(tween(Motion.SCREEN_MS)) { it / 6 } },
        ) {
            composable<CompetitionsRoute> {
                CompetitionListScreen(onOpen = { nav.navigate(CompetitionDetailRoute(it.value)) })
            }
            composable<CompetitionDetailRoute> { entry ->
                val route = entry.toRoute<CompetitionDetailRoute>()
                CompetitionDetailScreen(
                    competitionId = route.competitionId,
                    onOpenDeck = { listingId -> nav.navigate(DeckRoute(route.competitionId, listingId.value)) },
                    onOpenMatches = { nav.navigate(MatchesRoute(route.competitionId)) },
                    onOpenTeams = { nav.navigateToTab(Tab.TEAMS) },
                    onBack = nav::popBackStack,
                )
            }
            composable<DeckRoute> { entry ->
                val route = entry.toRoute<DeckRoute>()
                DeckScreen(
                    competitionId = route.competitionId,
                    listingId = route.listingId,
                    onOpenMatches = { nav.navigate(MatchesRoute(route.competitionId)) },
                    onBack = nav::popBackStack,
                )
            }
            composable<MatchesRoute> { entry ->
                MatchesScreen(entry.toRoute<MatchesRoute>().competitionId, onBack = nav::popBackStack)
            }
            composable<AllMatchesRoute> { MatchesScreen(competitionId = null, onBack = null) }
            composable<TeamsRoute> { TeamsScreen() }
            composable<ProfileRoute> { ProfileScreen(onEdit = { nav.navigate(EditProfileRoute) }) }
            composable<EditProfileRoute> {
                ProfileEditScreen(isSetup = false, onDone = nav::popBackStack, onBack = nav::popBackStack)
            }
        }
    }
}

/** Arcade pill tab bar: the selected tab sits in a Sun sticker and its icon hops when chosen. */
@Composable
private fun ArcadeNavBar(current: Tab, onSelect: (Tab) -> Unit) {
    val colors = Arcade.colors
    val reduced = Arcade.reducedMotion
    Column(Modifier.fillMaxWidth().background(colors.surface)) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(colors.line))
        Row(
            Modifier.navigationBarsPadding().padding(horizontal = 6.dp, vertical = 8.dp).height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Tab.entries.forEach { tab ->
                val selected = tab == current
                val fill by animateColorAsState(if (selected) SunSoft else Color.Transparent, tween(Motion.SCREEN_MS / 2), label = "tab-fill")
                val hop = remember { Animatable(1f) }
                LaunchedEffect(selected) {
                    if (selected && !reduced) {
                        hop.snapTo(0.7f)
                        hop.animateTo(1f, Motion.pop())
                    }
                }
                val shape = RoundedCornerShape(16.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(fill, shape)
                        .border(2.dp, if (selected) colors.line else Color.Transparent, shape)
                        .selectable(selected, remember { MutableInteractionSource() }, indication = null, role = Role.Tab) { onSelect(tab) },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val ink = if (selected) Ink else colors.muted
                    Icon(tab.icon, contentDescription = null, tint = ink, modifier = Modifier.size(24.dp).graphicsLayer { scaleX = hop.value; scaleY = hop.value })
                    Text(tab.label, color = ink, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp))
                }
            }
        }
    }
}
