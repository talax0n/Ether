package dev.ether.app

import dev.ether.app.data.AuthRepository
import dev.ether.app.data.SignUpResult
import dev.ether.app.data.fake.FakeBackend
import dev.ether.app.domain.AuthState
import dev.ether.app.domain.EtherError
import dev.ether.app.domain.ProfileId
import dev.ether.app.ui.signin.SignInMode
import dev.ether.app.ui.signin.SignInStep
import dev.ether.app.ui.signin.SignInViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val backend = FakeBackend()
    private val vm by lazy { SignInViewModel(backend) }
    private val me = AuthState.SignedIn(ProfileId("p-me"))

    private fun credentials(mode: SignInMode, password: String = "hunter22") {
        vm.continueWithEmail()
        vm.onModeChange(mode)
        vm.onEmailChange("ana@student.ac.id")
        vm.onPasswordChange(password)
    }

    /** An auth backend that only answers the calls a test needs. */
    private fun auth(signIn: () -> Unit = {}, signUp: () -> SignUpResult = { SignUpResult.SIGNED_IN }) = object : AuthRepository {
        override val authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
        override suspend fun signIn(email: String, password: String) = signIn()
        override suspend fun signUp(email: String, password: String) = signUp()
        override suspend fun signInWithGoogle(idToken: String, rawNonce: String) = Unit
        override suspend fun signOut() = Unit
    }

    @Test
    fun `signing in with a password starts a session`() = runTest {
        credentials(SignInMode.SIGN_IN)
        vm.submit()

        assertEquals(me, backend.authState.value)
        // Reset for the next sign-in after a sign-out.
        assertEquals(SignInStep.WELCOME, vm.state.value.step)
        assertEquals("", vm.state.value.email)
    }

    @Test
    fun `creating an account signs straight in`() = runTest {
        credentials(SignInMode.CREATE_ACCOUNT)
        vm.submit()

        assertEquals(me, backend.authState.value)
    }

    @Test
    fun `sign in needs a valid email and a password`() = runTest {
        vm.continueWithEmail()
        vm.onEmailChange("ana")
        vm.onPasswordChange("x")
        assertFalse(vm.state.value.canSubmit)

        vm.onEmailChange("ana@student.ac.id")
        vm.onPasswordChange("")
        assertFalse(vm.state.value.canSubmit)
    }

    @Test
    fun `creating an account asks for a 6+ character password`() = runTest {
        credentials(SignInMode.CREATE_ACCOUNT, password = "12345")
        assertFalse(vm.state.value.canSubmit)

        vm.onPasswordChange("123456")
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `if the project still confirms emails, sign-up says to open the link and switches to sign in`() = runTest {
        val vm = SignInViewModel(auth(signUp = { SignUpResult.VERIFY_EMAIL }))
        vm.continueWithEmail()
        vm.onModeChange(SignInMode.CREATE_ACCOUNT)
        vm.onEmailChange("ana@student.ac.id")
        vm.onPasswordChange("hunter22")
        vm.submit()

        assertEquals(SignInMode.SIGN_IN, vm.state.value.mode)
        assertEquals("Open the link we sent to ana@student.ac.id, then sign in.", vm.state.value.notice)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `a wrong password shows the error and stays signed out`() = runTest {
        val vm = SignInViewModel(auth(signIn = { throw EtherError.WrongCredentials() }))
        vm.continueWithEmail()
        vm.onEmailChange("ana@student.ac.id")
        vm.onPasswordChange("nope")
        vm.submit()

        assertEquals(EtherError.WrongCredentials().message, vm.state.value.error)
        assertEquals(SignInStep.CREDENTIALS, vm.state.value.step)
    }

    @Test
    fun `the welcome screen leads to email, and back returns to it`() = runTest {
        assertEquals(SignInStep.WELCOME, vm.state.value.step)
        vm.continueWithEmail()
        assertEquals(SignInStep.CREDENTIALS, vm.state.value.step)
        vm.back()
        assertEquals(SignInStep.WELCOME, vm.state.value.step)
    }

    @Test
    fun `a Google token signs in without the email steps`() = runTest {
        vm.signInWithGoogle("id-token", "nonce")

        assertEquals(me, backend.authState.value)
    }

    @Test
    fun `a failed Google sheet shows its message and stays on welcome`() = runTest {
        vm.googleFailed("No Google account on this phone. Add one in Settings, or use your email.")

        assertEquals(SignInStep.WELCOME, vm.state.value.step)
        assertEquals("No Google account on this phone. Add one in Settings, or use your email.", vm.state.value.error)
    }
}
