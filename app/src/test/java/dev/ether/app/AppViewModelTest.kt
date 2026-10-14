package dev.ether.app

import dev.ether.app.data.fake.DemoData
import dev.ether.app.data.fake.FakeBackend
import dev.ether.app.ui.AppUiState
import dev.ether.app.ui.AppViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun TestScope.stateFor(backend: FakeBackend): AppUiState {
        val vm = AppViewModel(backend, backend)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm.state.value
    }

    @Test
    fun `a complete profile goes straight to the app`() = runTest {
        assertEquals(AppUiState.Ready, stateFor(FakeBackend(signedIn = true)))
    }

    @Test
    fun `a profile missing its bio goes to profile setup`() = runTest {
        assertEquals(AppUiState.NeedsProfile(DemoData.me.id), stateFor(FakeBackend(signedIn = true, profile = DemoData.me.copy(bio = ""))))
    }

    @Test
    fun `a session whose account no longer exists is signed out`() = runTest {
        assertEquals(AppUiState.SignedOut, stateFor(FakeBackend(signedIn = true, profile = null)))
    }

    @Test
    fun `a profile missing its contact link goes to profile setup`() = runTest {
        assertEquals(AppUiState.NeedsProfile(DemoData.me.id), stateFor(FakeBackend(signedIn = true, profile = DemoData.me.copy(contactUrl = null))))
    }
}
