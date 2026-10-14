package dev.ether.app

import dev.ether.app.data.fake.FakeBackend
import dev.ether.app.domain.AuthState
import dev.ether.app.domain.ProfileId
import dev.ether.app.ui.signin.SignInStep
import dev.ether.app.ui.signin.SignInViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val backend = FakeBackend()
    private val vm by lazy { SignInViewModel(backend) }

    @Test
    fun `sending the code moves to the code step`() = runTest {
        vm.onEmailChange("ana@student.ac.id")
        vm.submit()

        assertEquals(SignInStep.CODE, vm.state.value.step)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `invalid email cannot be submitted`() = runTest {
        vm.onEmailChange("ana")

        assertFalse(vm.state.value.canSubmit)
        vm.submit()
        assertEquals(SignInStep.EMAIL, vm.state.value.step)
    }

    @Test
    fun `code input keeps only six digits`() = runTest {
        vm.onCodeChange("12a34-5678")

        assertEquals("123456", vm.state.value.code)
    }

    @Test
    fun `wrong code shows an error and stays signed out`() = runTest {
        vm.onEmailChange("ana@student.ac.id")
        vm.submit()
        vm.onCodeChange("000000")
        vm.submit()

        assertEquals("That code is wrong or has expired.", vm.state.value.error)
        assertEquals(AuthState.SignedOut, backend.authState.value)
    }

    @Test
    fun `correct code signs in`() = runTest {
        vm.onEmailChange("ana@student.ac.id")
        vm.submit()
        vm.onCodeChange("123456")
        vm.submit()

        assertEquals(AuthState.SignedIn(ProfileId("p-me")), backend.authState.value)
        // Reset for the next sign-in after a sign-out.
        assertEquals(SignInStep.EMAIL, vm.state.value.step)
        assertEquals("", vm.state.value.email)
    }
}
