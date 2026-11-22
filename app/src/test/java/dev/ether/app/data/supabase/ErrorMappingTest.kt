package dev.ether.app.data.supabase

import dev.ether.app.domain.EtherError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorMappingTest {
    @Test
    fun `postgres raise strings map to their domain errors`() {
        assertTrue(etherErrorFor("registration_closed") is EtherError.RegistrationClosed)
        assertTrue(etherErrorFor("already_in_team") is EtherError.AlreadyInTeam)
        assertTrue(etherErrorFor("listing_inactive") is EtherError.ListingInactive)
        assertTrue(etherErrorFor("team_to_team") is EtherError.TeamToTeam)
        assertTrue(etherErrorFor("swipe_cap_reached") is EtherError.SwipeCapReached)
        assertTrue(etherErrorFor("team_formed_after_deadline") is EtherError.TeamFormedAfterDeadline)
        assertTrue(etherErrorFor("result_already_confirmed") is EtherError.ResultAlreadyConfirmed)
    }

    @Test
    fun `registration_closed reads as a human sentence`() {
        assertEquals("Registration for this competition has closed.", etherErrorFor("registration_closed")?.message)
    }

    @Test
    fun `a generic sign-up failure is no longer blamed on the email domain`() {
        assertNull(etherErrorFor("unexpected_failure\nDatabase error saving new user"))
    }

    @Test
    fun `duplicate endorsement maps to already endorsed`() {
        val text = "duplicate key value violates unique constraint \"endorsements_from_profile_id_to_profile_id_competition_id_key\""
        assertEquals("You already endorsed this teammate.", etherErrorFor(text)?.message)
    }

    @Test
    fun `unknown text is not mapped`() {
        assertNull(etherErrorFor("permission denied for table profiles"))
    }
}
