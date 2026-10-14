package dev.ether.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.ether.app.ui.competitions.CompetitionDetailScreen
import dev.ether.app.ui.competitions.CompetitionListScreen
import dev.ether.app.ui.deck.DeckScreen
import dev.ether.app.ui.matches.MatchesScreen
import dev.ether.app.ui.profile.ProfileEditScreen
import dev.ether.app.ui.profile.ProfileScreen
import dev.ether.app.ui.signin.SignInScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object CompetitionsRoute
@Serializable data class CompetitionDetailRoute(val competitionId: String)
@Serializable data class DeckRoute(val competitionId: String, val listingId: String)
@Serializable data class MatchesRoute(val competitionId: String)
@Serializable data object ProfileRoute
@Serializable data object EditProfileRoute

@Composable
fun EtherRoot(vm: AppViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        AppUiState.Loading -> LoadingBox()
        AppUiState.SignedOut -> SignInScreen()
        AppUiState.NeedsProfile -> ProfileEditScreen(isSetup = true, onDone = vm::recheck, onBack = {})
        is AppUiState.Failed -> MessageBox(s.message, action = "Retry", onAction = vm::recheck)
        AppUiState.Ready -> MainNavHost()
    }
}

@Composable
private fun MainNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = CompetitionsRoute) {
        composable<CompetitionsRoute> {
            CompetitionListScreen(
                onOpen = { nav.navigate(CompetitionDetailRoute(it.value)) },
                onProfile = { nav.navigate(ProfileRoute) },
            )
        }
        composable<CompetitionDetailRoute> { entry ->
            val route = entry.toRoute<CompetitionDetailRoute>()
            CompetitionDetailScreen(
                competitionId = route.competitionId,
                onOpenDeck = { listingId -> nav.navigate(DeckRoute(route.competitionId, listingId.value)) },
                onOpenMatches = { nav.navigate(MatchesRoute(route.competitionId)) },
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
        composable<ProfileRoute> {
            ProfileScreen(onEdit = { nav.navigate(EditProfileRoute) }, onBack = nav::popBackStack)
        }
        composable<EditProfileRoute> {
            ProfileEditScreen(isSetup = false, onDone = nav::popBackStack, onBack = nav::popBackStack)
        }
    }
}
