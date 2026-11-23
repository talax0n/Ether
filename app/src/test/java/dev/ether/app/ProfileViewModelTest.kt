package dev.ether.app

import dev.ether.app.data.LevelMemory
import dev.ether.app.data.fake.DemoData
import dev.ether.app.data.fake.FakeBackend
import dev.ether.app.domain.HackathonResult
import dev.ether.app.domain.PastHackathon
import dev.ether.app.domain.PastHackathonId
import dev.ether.app.domain.TrackRecord
import dev.ether.app.domain.TrackRecordRules
import dev.ether.app.ui.profile.ProfileUiState
import dev.ether.app.ui.profile.ProfileViewModel
import dev.ether.app.ui.profile.TrackRecordEditor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private class Memory(override var lastSeenLevel: Int = 0) : LevelMemory

    private fun profileVm(backend: FakeBackend = FakeBackend(signedIn = true)) =
        ProfileViewModel(backend, backend, backend, Memory(), currentYear = { 2026 }).also { it.load() }

    private fun loaded(vm: ProfileViewModel) = vm.state.value as ProfileUiState.Loaded
    private fun hackathonEditor(vm: ProfileViewModel) = loaded(vm).editor as TrackRecordEditor.Hackathon
    private fun projectEditor(vm: ProfileViewModel) = loaded(vm).editor as TrackRecordEditor.Project

    @Test
    fun `profile loads the user's track record`() = runTest {
        val vm = profileVm()

        val record = loaded(vm).trackRecord
        assertEquals(listOf("GEMASTIK", "Campus Hackathon"), record.hackathons.map { it.name })
        assertEquals(listOf("Antri"), record.projects.map { it.title })
        assertNull(loaded(vm).editor)
    }

    @Test
    fun `adding a hackathon saves it and closes the sheet`() = runTest {
        val vm = profileVm()
        vm.addHackathon()
        assertEquals("2026", hackathonEditor(vm).form.year)
        assertFalse("name is required", loaded(vm).canSave)

        vm.editHackathonForm(hackathonEditor(vm).form.copy(name = "  Hackfest ", result = HackathonResult.WINNER, url = "devpost.com/hackfest"))
        assertTrue(loaded(vm).canSave)
        vm.save()

        assertNull(loaded(vm).editor)
        val added = loaded(vm).trackRecord.hackathons.first { it.name == "Hackfest" }
        assertEquals(HackathonResult.WINNER, added.result)
        assertEquals(2026, added.year)
        assertEquals("https://devpost.com/hackfest", added.url)
        assertNull(added.projectName)
    }

    @Test
    fun `the year must be between 2000 and next year`() = runTest {
        val vm = profileVm()
        vm.addHackathon()
        val form = hackathonEditor(vm).form.copy(name = "Old one")

        vm.editHackathonForm(form.copy(year = "1999"))
        assertFalse(loaded(vm).canSave)
        vm.editHackathonForm(form.copy(year = "2028"))
        assertFalse(loaded(vm).canSave)
        vm.editHackathonForm(form.copy(year = "2027"))
        assertTrue(loaded(vm).canSave)
    }

    @Test
    fun `http links are rejected`() = runTest {
        val vm = profileVm()
        vm.addProject()
        vm.editProjectForm(projectEditor(vm).form.copy(title = "Site", blurb = "A site.", url = "http://example.com"))
        assertTrue(projectEditor(vm).form.urlInvalid)
        assertFalse(loaded(vm).canSave)
    }

    @Test
    fun `editing a hackathon updates it in place`() = runTest {
        val vm = profileVm()
        val gemastik = loaded(vm).trackRecord.hackathons.first { it.name == "GEMASTIK" }
        vm.editHackathon(gemastik)
        assertEquals("Antri", hackathonEditor(vm).form.projectName)

        vm.editHackathonForm(hackathonEditor(vm).form.copy(result = HackathonResult.WINNER, projectName = ""))
        vm.save()

        val updated = loaded(vm).trackRecord.hackathons.single { it.id == gemastik.id }
        assertEquals(HackathonResult.WINNER, updated.result)
        assertNull("clearing the field stores null", updated.projectName)
        assertEquals(2, loaded(vm).trackRecord.hackathons.size)
    }

    @Test
    fun `deleting takes a second tap`() = runTest {
        val vm = profileVm()
        val antri = loaded(vm).trackRecord.projects.single()
        vm.editProject(antri)

        vm.delete()
        assertTrue(projectEditor(vm).confirmingDelete)
        assertEquals(1, loaded(vm).trackRecord.projects.size)

        vm.delete()
        assertNull(loaded(vm).editor)
        assertTrue(loaded(vm).trackRecord.projects.isEmpty())
    }

    @Test
    fun `a project needs a one-line blurb of at most 140 characters and at most 6 tags`() = runTest {
        val vm = profileVm()
        vm.addProject()
        val form = projectEditor(vm).form.copy(title = "Antri 2", blurb = "x".repeat(140), tags = "Kotlin, Compose, Kotlin")
        vm.editProjectForm(form)
        assertTrue(loaded(vm).canSave)
        assertEquals(listOf("Kotlin", "Compose"), projectEditor(vm).form.tagList)

        vm.editProjectForm(form.copy(blurb = "x".repeat(141)))
        assertFalse(loaded(vm).canSave)
        vm.editProjectForm(form.copy(tags = "a, b, c, d, e, f, g"))
        assertFalse(loaded(vm).canSave)

        vm.editProjectForm(form.copy(blurb = "Queue app\nfor clinics"))
        vm.save()
        assertEquals("Queue app for clinics", loaded(vm).trackRecord.projects.first().blurb)
    }

    @Test
    fun `a rejected save keeps the sheet open with the error`() = runTest {
        val full = TrackRecord(
            hackathons = (1..TrackRecordRules.MAX_ENTRIES).map {
                PastHackathon(PastHackathonId("h$it"), "H$it", 2024, HackathonResult.PARTICIPANT, null, null)
            },
            projects = emptyList(),
        )
        val vm = profileVm(FakeBackend(signedIn = true, trackRecord = full))
        assertFalse(loaded(vm).trackRecord.canAddHackathon)

        // The button is disabled at the cap, but the database is what refuses the 21st.
        vm.addHackathon()
        vm.editHackathonForm(hackathonEditor(vm).form.copy(name = "One more"))
        vm.save()

        val editor = hackathonEditor(vm)
        assertFalse(editor.saving)
        assertEquals("You can list up to 20 past hackathons. Remove one to add another.", editor.error)
        assertEquals(TrackRecordRules.MAX_ENTRIES, loaded(vm).trackRecord.hackathons.size)
    }

    @Test
    fun `closing the sheet discards the draft`() = runTest {
        val vm = profileVm()
        vm.addProject()
        vm.editProjectForm(projectEditor(vm).form.copy(title = "Draft"))
        vm.closeEditor()

        assertNull(loaded(vm).editor)
        assertEquals(DemoData.myTrackRecord.projects, loaded(vm).trackRecord.projects)
    }

    @Test
    fun `reloading on resume keeps an open sheet`() = runTest {
        val vm = profileVm()
        vm.addHackathon()
        vm.load()
        assertTrue(loaded(vm).editor is TrackRecordEditor.Hackathon)
    }
}
