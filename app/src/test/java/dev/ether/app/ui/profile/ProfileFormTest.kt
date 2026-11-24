package dev.ether.app.ui.profile

import dev.ether.app.domain.ContactKind
import dev.ether.app.domain.Contacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFormTest {
    private val complete = ProfileForm(
        name = "Ana", major = "DKV", year = "3", bio = "Designer", skills = listOf("Figma"), roles = listOf("Designer"),
    )

    @Test
    fun `at least one contact is required`() {
        assertNull(complete.toDraftOrNull())
        assertEquals(Contacts(lineId = "ana_l"), complete.copy(lineId = "ana_l").toDraftOrNull()?.contacts)
    }

    @Test
    fun `contacts are normalized into the draft`() {
        val draft = complete.copy(phone = "0812 3456 7890", instagram = "@Ana.Designs").toDraftOrNull()
        assertEquals(Contacts(phone = "6281234567890", instagram = "ana.designs"), draft?.contacts)
    }

    @Test
    fun `one invalid contact blocks saving even when another is valid`() {
        val form = complete.copy(phone = "0812 3456 7890", lineId = "ab")
        assertTrue(form.isInvalid(ContactKind.LINE))
        assertFalse(form.isInvalid(ContactKind.WHATSAPP))
        assertFalse(form.isInvalid(ContactKind.INSTAGRAM))
        assertNull(form.toDraftOrNull())
    }

    @Test
    fun `blank fields are not invalid`() {
        val form = complete.copy(instagram = "   ", lineId = "ana_l")
        assertFalse(form.isInvalid(ContactKind.INSTAGRAM))
        assertNotNull(form.toDraftOrNull())
    }
}
