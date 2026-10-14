package dev.ether.app.data.supabase

import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.DeckCard
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.ListingKind
import dev.ether.app.domain.MatchId
import dev.ether.app.domain.ProfileId
import dev.ether.app.domain.Reliability
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** Bodies copied verbatim from local PostgREST responses for the seed data in supabase/seed.sql. */
class ContractTest {
    // supabase-kt's default serializer ignores unknown keys; rows carry columns the DTOs don't map.
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deck solo row`() {
        val row = """[{"listing_id":"b0000000-0000-0000-0000-000000000002","competition_id":"c0000000-0000-0000-0000-000000000001","kind":"solo","profile_id":"d0000000-0000-0000-0000-000000000002","team_id":null,"owner_id":"d0000000-0000-0000-0000-000000000002","name":"Budi Santoso","headline":"Product thinker, ran two campus startups.","skills":["Figma","Research","Pitching"],"roles":["PM","Designer"],"open_roles":[],"member_count":1,"xp":300,"reliability_score":0,"avatar_url":null,"active":true,"created_at":"2026-10-14T19:55:30.815528+00:00"}]"""

        val card = json.decodeFromString<List<DeckRowDto>>(row).single().toDomain()

        assertEquals(
            DeckCard.Solo(
                ListingId("b0000000-0000-0000-0000-000000000002"), "Budi Santoso", null,
                "Product thinker, ran two campus startups.", listOf("Figma", "Research", "Pitching"),
                listOf("PM", "Designer"), 300, Reliability(0),
            ),
            card,
        )
    }

    @Test
    fun `deck team row`() {
        val row = """[{"listing_id":"af01b8a2-0e16-4407-8d5d-98ac8302435a","competition_id":"c0000000-0000-0000-0000-000000000001","kind":"team","profile_id":null,"team_id":"a0000000-0000-0000-0000-000000000001","owner_id":"d0000000-0000-0000-0000-000000000007","name":"Byte Club","headline":"Campus marketplace for used textbooks.","skills":["Canva","Finance","Pitching","React","Supabase","TypeScript"],"roles":["Backend","Business","Frontend","PM"],"open_roles":["Mobile","Designer"],"member_count":2,"xp":300,"reliability_score":0,"avatar_url":null,"active":true,"created_at":"2026-10-14T19:47:30.815528+00:00"}]"""

        val card = json.decodeFromString<List<DeckRowDto>>(row).single().toDomain()

        assertEquals(
            DeckCard.Team(
                ListingId("af01b8a2-0e16-4407-8d5d-98ac8302435a"), "Byte Club", null,
                "Campus marketplace for used textbooks.", listOf("Mobile", "Designer"), 2, 300, Reliability(0),
            ),
            card,
        )
    }

    @Test
    fun `my_matches row`() {
        val row = """[{"match_id":"4b49221f-7914-402b-ae87-b2e6ffb903ed","matched_at":"2026-10-14T20:01:26.761224+00:00","my_listing_id":"b0000000-0000-0000-0000-000000000001","listing_id":"af01b8a2-0e16-4407-8d5d-98ac8302435a","kind":"team","profile_id":null,"team_id":"a0000000-0000-0000-0000-000000000001","name":"Byte Club","headline":"Campus marketplace for used textbooks.","skills":["Canva","Finance","Go","Kotlin","Pitching","PostgreSQL","React","Supabase","TypeScript"],"roles":["Backend","Business","Frontend","PM"],"open_roles":["Mobile","Designer"],"member_count":3,"xp":750,"reliability_score":0,"avatar_url":null,"contact_url":"https://wa.me/628120000007"}]"""
        val competition = CompetitionId("c0000000-0000-0000-0000-000000000001")

        val match = json.decodeFromString<List<MatchRowDto>>(row).single().toDomain(competition)

        assertEquals(MatchId("4b49221f-7914-402b-ae87-b2e6ffb903ed"), match.id)
        assertEquals(competition, match.competitionId)
        assertEquals(ListingId("af01b8a2-0e16-4407-8d5d-98ac8302435a"), match.otherListingId)
        assertEquals(ListingKind.TEAM, match.otherKind)
        assertEquals("Byte Club", match.name)
        assertNull(match.avatarUrl)
        assertEquals("https://wa.me/628120000007", match.contactUrl)
        assertEquals(Instant.parse("2026-10-14T20:01:26.761224Z"), match.createdAt)
    }

    @Test
    fun `competition with null description`() {
        val row = """[{"id":"c0000000-0000-0000-0000-000000000002","name":"Data Challenge 2026","description":null,"url":"https://example.ac.id/data","registration_deadline":"2026-09-14T19:57:30.815528+00:00","event_end":"2026-10-07T19:57:30.815528+00:00","min_team_size":2,"max_team_size":3}]"""

        val c = json.decodeFromString<List<CompetitionDto>>(row).single().toDomain()

        assertEquals(CompetitionId("c0000000-0000-0000-0000-000000000002"), c.id)
        assertEquals("Data Challenge 2026", c.name)
        assertEquals("", c.description)
        assertEquals("https://example.ac.id/data", c.url)
        assertEquals(Instant.parse("2026-09-14T19:57:30.815528Z"), c.registrationDeadline)
        assertEquals(Instant.parse("2026-10-07T19:57:30.815528Z"), c.eventEnd)
        assertEquals(2, c.minTeamSize)
        assertEquals(3, c.maxTeamSize)
    }

    @Test
    fun `profile with null major and year`() {
        val row = """[{"id":"d0000000-0000-0000-0000-000000000005","name":"Eka Putri","major":null,"year":null,"bio":"Data science, Kaggle bronze.","skills":["Python","Pandas","PyTorch"],"roles":["Data","ML"],"avatar_url":null,"xp":50,"created_at":"2026-10-14T19:57:30.815528+00:00"}]"""

        val p = json.decodeFromString<List<ProfileDto>>(row).single().toDomain(contactUrl = "https://wa.me/628120000005")

        assertEquals(ProfileId("d0000000-0000-0000-0000-000000000005"), p.id)
        assertEquals("Eka Putri", p.name)
        assertNull(p.major)
        assertNull(p.year)
        assertEquals("Data science, Kaggle bronze.", p.bio)
        assertEquals(listOf("Python", "Pandas", "PyTorch"), p.skills)
        assertEquals(listOf("Data", "ML"), p.roles)
        assertEquals("https://wa.me/628120000005", p.contactUrl)
        assertEquals(50, p.xp)
    }

    @Test
    fun `swipe result with and without a match`() {
        val matched = json.decodeFromString<SwipeResultDto>("""{"matched": true, "match_id": "4b49221f-7914-402b-ae87-b2e6ffb903ed", "remaining": 29}""").toDomain()
        val missed = json.decodeFromString<SwipeResultDto>("""{"matched": false, "match_id": null, "remaining": 29}""").toDomain()

        assertEquals(true, matched.matched)
        assertEquals(MatchId("4b49221f-7914-402b-ae87-b2e6ffb903ed"), matched.matchId)
        assertEquals(29, matched.remaining)
        assertEquals(false, missed.matched)
        assertNull(missed.matchId)
        assertEquals(29, missed.remaining)
    }
}
