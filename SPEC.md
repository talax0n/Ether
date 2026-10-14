# Ether — Spec

> Ether is a gamified team-finder for university competitions. Each competition has its own swipe deck of solo students and teams with open slots. A mutual right swipe creates a match. Gamification rewards outcomes (teams formed, events finished, endorsements), not swipe volume.

## 1. Problem & target user

**User.** Students at one university who want to enter hackathons and competitions (e.g. GEMASTIK, campus hackathons, business case competitions) but lack a full team, or teams that need one more role.

**Problem.** Team finding today runs through WhatsApp group blasts and "anyone need a designer?" posts. Those are noisy, one-sided, and give no signal about whether a stranger will show up and contribute. Students outside a friend circle get left out.

## 2. Concept

- **Per-competition decks.** You swipe inside one competition, so every card shares your deadline and goal.
- **Two-sided consent.** A solo student swipes on teams and other solos. A team owner swipes on solos for the team. A match needs both sides to swipe right. Neither side gets cold-pitched.
- **Contact handoff.** A match reveals both contact links (WhatsApp / Telegram). Conversation happens where students already are.
- **Reputation.** After the competition ends, teammates endorse each other. The resulting reliability score shows on your card.
- **Outcome-based gamification.** XP and badges come from forming teams, finishing events, and receiving endorsements. A daily swipe cap keeps swipes deliberate.

**Alternatives considered.** Solo-to-solo only was rejected because existing teams that need one role are the common case. One-sided apply with an inbox was rejected because it loses the mutual-consent feel. In-app chat was rejected because it doubles scope.

## 3. Domain model

The key idea is the **Listing**. A Listing is "this solo user, or this team, is looking within this competition." Swipes and matches happen between listings. That gives solo and team cards one code path.

```
Profile      (id = auth.uid, name, major, year, bio, skills[], roles[], contactUrl, avatarUrl, xp)
Competition  (id, name, description, url, registrationDeadline, eventEnd, minTeamSize, maxTeamSize)
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

**Kotlin side.** Mirror the enums as sealed types (`ListingKind`, `SwipeDirection`). Parse Supabase rows into domain types at the repository boundary. UI code never sees raw JSON or nullable `profileId`/`teamId` pairs.

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

## 5. MVP features (YAGNI-tight)

**In scope:**
- Email OTP sign-in limited to the university domain.
- Profile create and edit: skills, roles, major, year, bio, contact link, avatar.
- Competition list (seeded by hand in the database).
- Join a competition as SOLO, or create a team and list it as TEAM.
- Swipe deck per competition with drag, tilt, fling, and like/nope stamp, plus buttons for accessibility.
- Mutual match → match screen with the other side's contact link.
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
- Multiple universities. One domain only.
- Recommendation or AI ranking. Deck order is newest-first, filtered by open roles.
- Admin dashboard. Competitions are seeded through the Supabase SQL editor.
- Leaderboards. They need a user base big enough for ranking to mean anything.
- Team-to-team merging.
- Push notifications. 🟡 Likely the first post-MVP addition, because a match you don't see is a lost match.

## 8. Risks & unknowns

- 🔴 **Domain-restricted sign-up.** Supabase does not restrict email domains out of the box. Plan: a "before user created" auth hook or a trigger on `auth.users` that rejects other domains. Verify this on day 1.
- 🔴 **University email domain.** TBD. Needed before auth can work.
- 🟡 **Cold start.** An empty deck kills the app. Mitigation: launch tied to one specific upcoming competition, seeded with friends and a campus org.
- 🟡 **Contact link abuse.** Contact links are only visible to matched listings. Enforce that with RLS, not in the client.
- 🟡 **Swipe gesture feel.** The hand-rolled card needs tuning (threshold, rotation, spring). Prototype it early on a real device.
- 🟢 **Matching logic.** It is a single trigger guarded by unique constraints. Low risk, cover it with SQL tests.
- 🟢 **Anon key in the APK.** The Supabase anon key is public by design. Security comes from RLS. The service role key never ships in the app.

## 9. Launch checklist

- [ ] University domain confirmed and enforced at sign-up
- [ ] RLS on every table, with a test that a non-matched user cannot read another user's `contactUrl`
- [ ] Matching, swipe cap, and XP triggers covered by SQL tests
- [ ] One real upcoming competition seeded
- [ ] Signed release APK, or a Play Store internal testing track
- [ ] 10+ seed users before the public announcement
