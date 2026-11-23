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
import kotlinx.coroutines.test.advanceTimeBy
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
        vm.onModeChange(mode)
        vm.onEmailChange("ana@student.ac.id")
        vm.onPasswordChange(password)
    }

    @Test
    fun `signing in with a password starts a session without a code`() = runTest {
        credentials(SignInMode.SIGN_IN)
        vm.submit()

        assertEquals(me, backend.authState.value)
        // Reset for the next sign-in after a sign-out.
        assertEquals(SignInStep.CREDENTIALS, vm.state.value.step)
        assertEquals("", vm.state.value.email)
    }

    @Test
    fun `sign in needs a valid email and a password`() = runTest {
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
    fun `creating an account moves to email verification`() = runTest {
        credentials(SignInMode.CREATE_ACCOUNT)
        vm.submit()

        assertEquals(SignInStep.VERIFY, vm.state.value.step)
        assertNull(vm.state.value.error)
        assertEquals(AuthState.SignedOut, backend.authState.value)
    }

    @Test
    fun `code input keeps only six digits`() = runTest {
        vm.onCodeChange("12a34-5678")

        assertEquals("123456", vm.state.value.code)
    }

    @Test
    fun `wrong code shows an error and stays signed out`() = runTest {
        credentials(SignInMode.CREATE_ACCOUNT)
        vm.submit()
        vm.onCodeChange("000000")

        assertEquals("That code is wrong or has expired.", vm.state.value.error)
        assertEquals(AuthState.SignedOut, backend.authState.value)
    }

    @Test
    fun `typing the last digit of the right code verifies and signs in`() = runTest {
        credentials(SignInMode.CREATE_ACCOUNT)
        vm.submit()
        vm.onCodeChange("123456")

        assertEquals(me, backend.authState.value)
    }

    @Test
    fun `signing in to an unconfirmed account resends the code and asks for it`() = runTest {
        var resent = 0
        val unconfirmed = object : AuthRepository {
            override val authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
            override suspend fun signIn(email: String, password: String) = throw EtherError.EmailNotConfirmed()
            override suspend fun signUp(email: String, password: String) = SignUpResult.VERIFY_EMAIL
            override suspend fun verifyEmail(email: String, code: String) = Unit
            override suspend fun resendVerification(email: String) { resent++ }
            override suspend fun signInWithGoogle(idToken: String, rawNonce: String) = Unit
            override suspend fun signOut() = Unit
        }
        val vm = SignInViewModel(unconfirmed)
        vm.onEmailChange("ana@student.ac.id")
        vm.onPasswordChange("hunter22")
        vm.submit()

        assertEquals(SignInStep.VERIFY, vm.state.value.step)
        assertEquals(1, resent)
    }

    @Test
    fun `back from verification returns to the credentials`() = runTest {
        credentials(SignInMode.CREATE_ACCOUNT)
        vm.submit()
        vm.back()

        assertEquals(SignInStep.CREDENTIALS, vm.state.value.step)
        assertEquals("ana@student.ac.id", vm.state.value.email)
    }

    @Test
    fun `resend waits out the cooldown, then clears the code and restarts it`() = runTest(main.dispatcher) {
        credentials(SignInMode.CREATE_ACCOUNT)
        vm.submit()
        assertEquals(30, vm.state.value.resendIn)

        vm.onCodeChange("12")
        vm.resend()
        assertEquals("12", vm.state.value.code)

        advanceTimeBy(30_001)
        assertEquals(0, vm.state.value.resendIn)
        vm.resend()
        assertEquals("", vm.state.value.code)
        assertEquals(30, vm.state.value.resendIn)
    }

    @Test
    fun `a Google token signs in without the email steps`() = runTest {
        vm.signInWithGoogle("id-token", "nonce")

        assertEquals(me, backend.authState.value)
    }

    @Test
    fun `a failed Google sheet shows its message and stays on credentials`() = runTest {
        vm.googleFailed("No Google account on this phone. Add one in Settings, or use your email.")

        assertEquals(SignInStep.CREDENTIALS, vm.state.value.step)
        assertEquals("No Google account on this phone. Add one in Settings, or use your email.", vm.state.value.error)
    }
}
