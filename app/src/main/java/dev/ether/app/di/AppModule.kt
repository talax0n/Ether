package dev.ether.app.di

import dev.ether.app.BuildConfig
import dev.ether.app.data.AuthRepository
import dev.ether.app.data.CompetitionRepository
import dev.ether.app.data.DeckRepository
import dev.ether.app.data.MatchRepository
import dev.ether.app.data.ProfileRepository
import dev.ether.app.data.TeamRepository
import dev.ether.app.data.fake.FakeBackend
import dev.ether.app.data.supabase.SupabaseAuthRepository
import dev.ether.app.data.supabase.SupabaseCompetitionRepository
import dev.ether.app.data.supabase.SupabaseDeckRepository
import dev.ether.app.data.supabase.SupabaseMatchRepository
import dev.ether.app.data.supabase.SupabaseProfileRepository
import dev.ether.app.data.supabase.SupabaseTeamRepository
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.ListingId
import dev.ether.app.ui.AppViewModel
import dev.ether.app.ui.competitions.CompetitionDetailViewModel
import dev.ether.app.ui.competitions.CompetitionListViewModel
import dev.ether.app.ui.deck.DeckViewModel
import dev.ether.app.ui.matches.MatchesViewModel
import dev.ether.app.ui.profile.ProfileEditViewModel
import dev.ether.app.ui.profile.ProfileViewModel
import dev.ether.app.ui.signin.SignInViewModel
import dev.ether.app.ui.teams.TeamsViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

private val fakeData = module {
    single { FakeBackend() } binds arrayOf(
        AuthRepository::class, ProfileRepository::class, CompetitionRepository::class,
        DeckRepository::class, MatchRepository::class, TeamRepository::class,
    )
}

private val supabaseData = module {
    single {
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
            install(Auth)
            install(Postgrest)
        }
    }
    single<AuthRepository> { SupabaseAuthRepository(get()) }
    single<ProfileRepository> { SupabaseProfileRepository(get()) }
    single<CompetitionRepository> { SupabaseCompetitionRepository(get()) }
    single<DeckRepository> { SupabaseDeckRepository(get()) }
    single<MatchRepository> { SupabaseMatchRepository(get()) }
    single<TeamRepository> { SupabaseTeamRepository(get()) }
}

private val viewModels = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::SignInViewModel)
    viewModelOf(::ProfileEditViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::CompetitionListViewModel)
    viewModel { (id: String) -> CompetitionDetailViewModel(CompetitionId(id), get(), get()) }
    viewModel { (competitionId: String, listingId: String) ->
        DeckViewModel(CompetitionId(competitionId), ListingId(listingId), get(), get())
    }
    // No parameter: matches across every competition (the Matches tab).
    viewModel { params -> MatchesViewModel(params.getOrNull<String>()?.let(::CompetitionId), get(), get()) }
    viewModel { TeamsViewModel(get()) }
}

val appModules = listOf(if (BuildConfig.USE_FAKE_BACKEND) fakeData else supabaseData, viewModels)
