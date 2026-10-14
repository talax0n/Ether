package dev.ether.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.ether.app.R
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

private enum class Tab(val route: Any, val routeClass: KClass<*>, val label: String, @DrawableRes val icon: Int) {
    COMPETITIONS(CompetitionsRoute, CompetitionsRoute::class, "Competitions", R.drawable.ic_trophy),
    MATCHES(AllMatchesRoute, AllMatchesRoute::class, "Matches", R.drawable.ic_favorite),
    TEAMS(TeamsRoute, TeamsRoute::class, "Teams", R.drawable.ic_group),
    PROFILE(ProfileRoute, ProfileRoute::class, "Profile", R.drawable.ic_person),
}

@Composable
fun EtherRoot(vm: AppViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        AppUiState.Loading -> LoadingBox()
        AppUiState.SignedOut -> SignInScreen()
        // Keyed by user: this screen lives outside the NavHost, so its ViewModel would otherwise outlive a sign-out.
        is AppUiState.NeedsProfile -> ProfileEditScreen(
            isSetup = true, onDone = vm::recheck, onBack = {},
            vm = koinViewModel(key = "profile-setup-${s.userId.value}"),
        )
        is AppUiState.Failed -> MessageBox(s.message, action = "Retry", onAction = vm::recheck)
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
    Scaffold(
        bottomBar = {
            // Only on the four top-level screens; detail, deck and edit screens get the whole height.
            if (currentTab != null) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { nav.navigateToTab(tab) },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = CompetitionsRoute, modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
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
