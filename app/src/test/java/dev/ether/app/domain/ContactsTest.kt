package dev.ether.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsTest {
    @Test
    fun `an Indonesian 08 number becomes 628`() {
        assertEquals("6281234567890", Contacts.normalizePhone("081234567890"))
        assertEquals("6281234567890", Contacts.normalizePhone("0812-3456-7890"))
        assertEquals("6281234567890", Contacts.normalizePhone(" 0812 3456 7890 "))
    }

    @Test
    fun `a number with a country code keeps it and loses the plus`() {
        assertEquals("6281234567890", Contacts.normalizePhone("+62 812 3456 7890"))
        assertEquals("6281234567890", Contacts.normalizePhone("6281234567890"))
        assertEquals("15551234567", Contacts.normalizePhone("+1 (555) 123-4567"))
    }

    @Test
    fun `invalid phone numbers are rejected`() {
        assertNull(Contacts.normalizePhone(""))
        assertNull(Contacts.normalizePhone("1234567"))            // 7 digits
        assertNull(Contacts.normalizePhone("12345678901234567"))  // 17 digits
        assertNull(Contacts.normalizePhone("0812abc4567"))
        assertNull(Contacts.normalizePhone("++6281234567"))
        assertNull(Contacts.normalizePhone("https://wa.me/6281234567890"))
    }

    @Test
    fun `instagram handles lose the @ and are lowercased`() {
        assertEquals("ana.designs", Contacts.normalizeInstagram("@ana.designs"))
        assertEquals("ana_designs", Contacts.normalizeInstagram("  Ana_Designs "))
        assertEquals("ana.designs", Contacts.normalizeInstagram("https://www.instagram.com/ana.designs/"))
        assertEquals("a".repeat(30), Contacts.normalizeInstagram("a".repeat(30)))
    }

    @Test
    fun `invalid instagram handles are rejected`() {
        assertNull(Contacts.normalizeInstagram("@"))
        assertNull(Contacts.normalizeInstagram("ana designs"))
        assertNull(Contacts.normalizeInstagram("ana-designs"))
        assertNull(Contacts.normalizeInstagram("@@ana"))
        assertNull(Contacts.normalizeInstagram("a".repeat(31)))
    }

    @Test
    fun `line ids are lowercased and checked`() {
        assertEquals("budi.s-01_x", Contacts.normalizeLineId(" Budi.S-01_x "))
        assertNull(Contacts.normalizeLineId("abc"))
        assertNull(Contacts.normalizeLineId("a".repeat(21)))
        assertNull(Contacts.normalizeLineId("@budi"))
        assertNull(Contacts.normalizeLineId("budi s"))
    }

    @Test
    fun `entries list each contact with its display text and deep link`() {
        val contacts = Contacts(phone = "6281234567890", instagram = "ana.designs", lineId = "ana_l")
        assertEquals(
            listOf(
                ContactEntry(ContactKind.WHATSAPP, "+6281234567890", "https://wa.me/6281234567890"),
                ContactEntry(ContactKind.INSTAGRAM, "@ana.designs", "https://instagram.com/ana.designs"),
                ContactEntry(ContactKind.LINE, "ana_l", "https://line.me/ti/p/~ana_l"),
            ),
            contacts.entries,
        )
        assertEquals(listOf(ContactKind.INSTAGRAM), Contacts(instagram = "x").entries.map { it.kind })
        assertTrue(Contacts.NONE.entries.isEmpty())
    }

    @Test
    fun `stored rows are normalized and invalid values dropped`() {
        assertEquals(Contacts(phone = "6281234567890"), Contacts.of("+6281234567890", " ", null))
        assertTrue(Contacts.of(null, "not valid!", "ab").isEmpty)
    }
}
