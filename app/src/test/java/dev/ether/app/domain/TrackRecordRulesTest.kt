package dev.ether.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackRecordRulesTest {
    @Test
    fun `links are https only, and a bare host gets https`() {
        assertEquals("https://github.com/ether", TrackRecordRules.normalizeUrl(" github.com/ether "))
        assertEquals("https://devpost.com/x?y=1#z", TrackRecordRules.normalizeUrl("HTTPS://devpost.com/x?y=1#z"))
        assertNull(TrackRecordRules.normalizeUrl("http://example.com"))
        assertNull(TrackRecordRules.normalizeUrl("ftp://example.com"))
        assertNull(TrackRecordRules.normalizeUrl("javascript:alert(1)"))
        assertNull(TrackRecordRules.normalizeUrl("https://"))
        assertNull(TrackRecordRules.normalizeUrl("localhost"))
        assertNull(TrackRecordRules.normalizeUrl("https://exa mple.com"))
    }

    @Test
    fun `tags are trimmed, de-duplicated and capped`() {
        assertEquals(listOf("Kotlin", "Go"), TrackRecordRules.splitTags(" Kotlin, ,Go, Kotlin"))
        assertTrue(TrackRecordRules.tagsValid(List(6) { "t$it" }))
        assertFalse(TrackRecordRules.tagsValid(List(7) { "t$it" }))
        assertFalse(TrackRecordRules.tagsValid(listOf("x".repeat(31))))
    }
}
