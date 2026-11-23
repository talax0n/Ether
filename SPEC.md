# Ether — Spec

> Ether is a gamified team-finder for university competitions. Each competition has its own swipe deck of solo students and teams with open slots. A mutual right swipe creates a match. Gamification rewards outcomes (teams formed, events finished, endorsements), not swipe volume.

## 1. Problem & target user

**User.** Students who want to enter hackathons and competitions (e.g. GEMASTIK, campus hackathons, business case competitions) but lack a full team, or teams that need one more role.

**Problem.** Team finding today runs through WhatsApp group blasts and "anyone need a designer?" posts. Those are noisy, one-sided, and give no signal about whether a stranger will show up and contribute. Students outside a friend circle get left out.

## 2. Concept

- **Per-competition decks.** You swipe inside one competition, so every card shares your deadline and goal.
- **Two-sided consent.** A solo student swipes on teams and other solos. A team owner swipes on solos for the team. A match needs both sides to swipe right. Neither side gets cold-pitched.
- **Contact handoff.** A match reveals both sides' contacts: a phone number (opened in WhatsApp), an Instagram handle, and/or a LINE ID. Conversation happens where students already are.
- **Reputation.** After the competition ends, teammates endorse each other. The resulting reliability score shows on your card.
- **Outcome-based gamification.** XP and badges come from forming teams, finishing events, and receiving endorsements. A daily swipe cap keeps swipes deliberate.

**Alternatives considered.** Solo-to-solo only was rejected because existing teams that need one role are the common case. One-sided apply with an inbox was rejected because it loses the mutual-consent feel. In-app chat was rejected because it doubles scope.

## 3. Domain model

The key idea is the **Listing**. A Listing is "this solo user, or this team, is looking within this competition." Swipes and matches happen between listings. That gives solo and team cards one code path.

```
Profile      (id = auth.uid, name, major, year, bio, skills[], roles[], contacts, avatarUrl, xp)
Contacts     (phone?, instagram?, lineId?)               at least one for a complete profile
Competition  (id, name, description, url, registrationDeadline, eventEnd, minTeamSize, maxTeamSize,
              organizer?, prize?, mode: ONLINE | OFFLINE | HYBRID, city?, tags[])
Team         (id, competitionId, ownerId, name, pitch, openRoles[])
TeamMember   (teamId, profileId)                         unique(teamId, profileId)
Listing      (id, competitionId, kind: SOLO | TEAM, profileId?, teamId?, active)
               exactly one of profileId / teamId set, matching kind
               unique(competitionId, profileId), unique(competitionId, teamId)
Swipe        (id, fromListingId, toListingId, direction: LEFT | RIGHT, swipedBy, createdAt)
               unique(fromListingId, toListingId)
Match        (id, competitionId, listingA, listingB, createdAt)
               unique(least(a,b), greatest(a,b))
Endorsement  (fromProfileId, toProfileId, competitionId, positive)
               unique(fromProfileId, toProfileId, competitionId)
PastHackathon (id, profileId, name, year, result: PARTICIPANT | HONORABLE_MENTION | FINALIST | WINNER, projectName?, url?)
Project      (id, profileId, title, blurb, url?, tags[])
Badge        (code, name, description)                  static table
ProfileBadge (profileId, badgeCode, awardedAt)          unique(profileId, badgeCode)
```

**Swipe rules**

| From \ To | SOLO | TEAM |
|-----------|------|------|
| SOLO | allowed (match → the two form a new team) | allowed (match → solo joins team) |
| TEAM | allowed (match → solo joins team) | not allowed (team merging out of scope) |

- Only a team's owner swipes for a TEAM listing.
- Matching runs in Postgres. On insert of a RIGHT swipe, if the reverse RIGHT swipe exists, insert a Match. The unique constraints make retries idempotent.
- Daily cap of 30 swipes per profile, enforced in the database. The client only displays the remaining count.
- A listing goes inactive when the team reaches `maxTeamSize` or the solo joins a team.

**Contacts.** Each is optional, but a complete profile needs at least one. Stored normalized: `phone` is 8–16 digits with the country code (an Indonesian `08…` is entered as is and stored as `628…`), `instagram` is 1–30 letters, digits, dots or underscores without the `@`, `lineId` is 4–20 letters, digits, dots, underscores or hyphens. Each opens its app: `https://wa.me/<phone>`, `https://instagram.com/<handle>`, `https://line.me/ti/p/~<id>`. Telegram is not supported.

**Track record.** A profile lists past hackathons and projects, so potential teammates see what someone has built. Self-reported and unverified, unlike a confirmed team result (§4); it earns no XP.
- Past hackathon: name (1–80 chars), year (2000 to next year), result, optional project name (1–80), optional link.
- Project: title (1–80), a one-line blurb (1–140), optional link, up to 6 tech tags (1–30 chars each).
- Links are `https://` only. At most 20 hackathons and 20 projects per profile, enforced by a trigger.
- Every signed-in user can read them; only the owner writes. Guests cannot read them.
- The deck card shows a summary: the best result (best result first, then the most recent) with that hackathon's name and year, plus the hackathon and project counts. A team card summarizes all current members.

**Competition details.** Hackathons are entered by hand by the team; there is no scraping. Each has an optional organizer (1–80 chars), a free-text prize as written (1–80, e.g. "Rp 50.000.000" or "$10,000 + swag"), a mode (online, offline or hybrid; default online), a city for offline and hybrid events (1–60; an online event has none), and up to 8 topic tags (1–30 chars each, unique ignoring case, e.g. AI, Web3, Mobile, Data, Social impact). The official page `url` is `https://` only. Guests can read all of it.

**Kotlin side.** Mirror the enums as sealed types (`ListingKind`, `SwipeDirection`, `ContactKind`, `HackathonResult`, `CompetitionMode`). Parse Supabase rows into domain types at the repository boundary. UI code never sees raw JSON or nullable `profileId`/`teamId` pairs.

## 4. Gamification

| Trigger | XP | Badge |
|---------|----|-------|
| Profile completed (all required fields + contact) | 50 | — |
| First match | 25 | — |
| Team formed / joined | 100 | First Team (first time) |
| Competition marked finished | 150 | 3 Hackathons (at 3) |
| Positive endorsement received | 30 | Reliable (at 5) |
| Self-reported result, confirmed by a teammate | 200 | Finalist / Winner |

XP is computed by database triggers so clients cannot grant themselves points. No leaderboard in the MVP.

**Levels** are display-only and derived from total XP on the client. No column or trigger stores them, so the database stays the single source of truth for XP.

| Level | Title | Total XP |
|-------|-------|----------|
| 1 | Rookie | 0 |
| 2 | Hacker | 100 |
| 3 | Maker | 250 |
| 4 | Builder | 400 |
| 5 | Captain | 600 |
| 6 | Architect | 850 |
| 7 | Mentor | 1150 |
| 8 | Legend | 1500 |

The profile shows the level, its title, and progress to the next threshold. Level 8 is the cap.

**Visual direction:** Arcade with two Neon Night elements (monospace numerals, a 30-tick swipe meter). See `docs/design/directions/arcade.html` and `docs/design/references.md`.

## 5. MVP features (YAGNI-tight)

**In scope:**
- Browse without an account: the intro, then the competition list and details. Sign-in is asked for only when the user acts (joining, starting a team, swiping) or opens Matches, Teams, or Profile.
- Sign in with email and password, or a Google account. Creating an account with email signs straight in; there are no email codes.
- Profile create and edit: skills, roles, major, year, bio, contacts (phone for WhatsApp, Instagram, LINE ID; at least one), avatar.
- Track record on the profile: add, edit and delete past hackathons and projects. The deck card shows the best result and the project count.
- Competition list (seeded by hand in the database), grouped into Open for sign-up, Registration closed and Finished. Cards show the mode, city and prize.
- Search and filter the list: a search box matching name, organizer, tags and city (case-insensitive, every word must match), and filter chips for status (Open / Closed / Finished), mode (Online / Offline / Hybrid) and the tags present in the data. Chips in one group are alternatives; groups combine. Filtering runs on the client over the loaded list. When nothing matches, the list says so and offers "Clear filters".
- Competition detail: organizer, prize, mode and city, tags, and an "Open official page" button showing the link's host that opens it in the browser.
- Join a competition as SOLO, or create a team and list it as TEAM.
- Swipe deck per competition with drag, tilt, fling, and like/nope stamp, plus buttons for accessibility.
- Mutual match → match screen with one button per contact the other side shared (WhatsApp, Instagram, LINE).
- Matches list per competition.
- Team membership update on accept (solo joins team, or two solos form a team).
- Post-event endorsement prompt.
- XP, badges, and reliability score on the profile and card.

**Not in MVP.** See §7.

## 6. Architecture

**Stack**
- Kotlin, Jetpack Compose, Material 3 (dynamic color off, custom brand theme).
- Navigation Compose. Koin for DI. Coil for images.
- `supabase-kt` (Auth, Postgrest, Storage).
- Single `app` module. Split into modules only when build time hurts.
- Swipe card hand-rolled with `Modifier.pointerInput` + `Animatable`. No third-party swipe library.

**Layering**
```
ui (Compose screens + ViewModels, StateFlow<UiState>)
   │
domain (pure Kotlin types: Profile, Listing, Match, ...)
   │
data (repositories; Supabase calls; row → domain parsing)
   │
Supabase: Postgres + RLS + triggers (matching, XP, swipe cap)
```

**Swipe flow**
```
User drags card ──► fling past threshold ──► SwipeRepository.insert(from, to, dir)
                                                 │
                              Postgres trigger: reverse RIGHT exists?
                                   │ yes                     │ no
                              insert Match               nothing
                                   │
            client reads insert result (matched: Boolean) ──► show match screen
```

Business rules live in Postgres (constraints, triggers, RLS) because they must hold for any client. The app stays a thin, well-typed view over them.

## 7. Non-goals

- In-app chat. Contact handoff covers it.
- iOS. Android first. Compose Multiplatform later if there is demand.
- Recommendation or AI ranking. Deck order is newest-first, filtered by open roles.
- Admin dashboard. Competitions are seeded through the Supabase SQL editor.
- Leaderboards. They need a user base big enough for ranking to mean anything.
- Team-to-team merging.
- Push notifications. 🟡 Likely the first post-MVP addition, because a match you don't see is a lost match.

## 8. Risks & unknowns

- 🟡 **Open sign-up.** Anyone with an email or a Google account can join, so the deck is no longer limited to one campus. Trust comes from per-competition decks, mutual consent, and endorsements. Revisit if decks fill with people who never show up.
- 🟡 **Google sign-in setup.** Needs a Google Cloud OAuth web client ID (shared with Supabase) and an Android client registered with the app's signing SHA-1. Without them the Google button stays hidden.
- 🟡 **Cold start.** An empty deck kills the app. Mitigation: launch tied to one specific upcoming competition, seeded with friends and a campus org.
- 🟡 **Contact abuse.** Contacts are only visible to matched listings and teammates. Enforce that with RLS, not in the client.
- 🟡 **Swipe gesture feel.** The hand-rolled card needs tuning (threshold, rotation, spring). Prototype it early on a real device.
- 🟢 **Matching logic.** It is a single trigger guarded by unique constraints. Low risk, cover it with SQL tests.
- 🟢 **Anon key in the APK.** The Supabase anon key is public by design. Security comes from RLS. The service role key never ships in the app.

## 9. Launch checklist

- [ ] Google sign-in configured for the release signing key
- [ ] RLS on every table, with a test that a non-matched user cannot read another user's contacts
- [ ] Matching, swipe cap, and XP triggers covered by SQL tests
- [ ] One real upcoming competition seeded
- [ ] Signed release APK, or a Play Store internal testing track
- [ ] 10+ seed users before the public announcement
