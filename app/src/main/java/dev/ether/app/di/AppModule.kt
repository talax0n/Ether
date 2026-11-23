package dev.ether.app.di

import android.os.SystemClock
import dev.ether.app.BuildConfig
import dev.ether.app.data.fake.FakeNearbyPeers
import dev.ether.app.data.nearby.AccelerometerShakeDetector
import dev.ether.app.data.nearby.GoogleNearbyPeers
import dev.ether.app.data.nearby.NearbyPeers
import dev.ether.app.data.nearby.ShakeDetector
import dev.ether.app.ui.nearby.NearbyViewModel
import dev.ether.app.data.AuthRepository
import dev.ether.app.data.CompetitionRepository
import dev.ether.app.data.DeckRepository
import dev.ether.app.data.MatchRepository
import dev.ether.app.data.DeviceStore
import dev.ether.app.data.ProfileRepository
import dev.ether.app.data.TeamRepository
import dev.ether.app.data.TrackRecordRepository
import dev.ether.app.data.LevelMemory
import dev.ether.app.data.fake.FakeBackend
import dev.ether.app.data.supabase.SupabaseAuthRepository
import dev.ether.app.data.supabase.SupabaseCompetitionRepository
import dev.ether.app.data.supabase.SupabaseDeckRepository
import dev.ether.app.data.supabase.SupabaseMatchRepository
import dev.ether.app.data.supabase.SupabaseProfileRepository
import dev.ether.app.data.supabase.SupabaseTeamRepository
import dev.ether.app.data.supabase.SupabaseTrackRecordRepository
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
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

private val fakeData = module {
    single { FakeBackend() } binds arrayOf(
        AuthRepository::class, ProfileRepository::class, CompetitionRepository::class,
        DeckRepository::class, MatchRepository::class, TeamRepository::class, TrackRecordRepository::class,
    )
    factory<NearbyPeers> { FakeNearbyPeers() }
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
    single<TrackRecordRepository> { SupabaseTrackRecordRepository(get()) }
    factory<NearbyPeers> { GoogleNearbyPeers(androidContext(), SystemClock::elapsedRealtime) }
}

private val viewModels = module {
    single { DeviceStore(androidContext()) } bind LevelMemory::class
    viewModelOf(::AppViewModel)
    viewModelOf(::SignInViewModel)
    viewModelOf(::ProfileEditViewModel)
    viewModel { ProfileViewModel(get(), get(), get(), get()) }
    viewModel { CompetitionListViewModel(get()) }
    viewModel { (id: String) -> CompetitionDetailViewModel(CompetitionId(id), get(), get()) }
    viewModel { (competitionId: String, listingId: String) ->
        DeckViewModel(CompetitionId(competitionId), ListingId(listingId), get(), get(), get())
    }
    // No parameter: matches across every competition (the Matches tab).
    viewModel { params -> MatchesViewModel(params.getOrNull<String>()?.let(::CompetitionId), get(), get()) }
    viewModel { TeamsViewModel(get()) }
    single<ShakeDetector> { AccelerometerShakeDetector(androidContext()) }
    viewModel { (competitionId: String, listingId: String) ->
        NearbyViewModel(
            CompetitionId(competitionId), ListingId(listingId), get(), get(), get(), get(), get(), get(), get(),
            clock = SystemClock::elapsedRealtime,
        )
    }
}

val appModules = listOf(if (BuildConfig.USE_FAKE_BACKEND) fakeData else supabaseData, viewModels)
