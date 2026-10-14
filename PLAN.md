# Ether — Plan

**Window:** start TBD → launch target TBD 🟡 (pick a real campus competition and launch 2–3 weeks before its registration deadline). Rough size is ~6 weeks part-time for one builder. Assumes the [SPEC.md](SPEC.md) scope with no additions.

## Setup (Week 0)

- Supabase project created. Env names live in `.env.example`: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `ALLOWED_EMAIL_DOMAIN`. Gradle reads them into `BuildConfig`. Real values stay in gitignored `local.properties` or `.env`.
- Android Studio (latest stable), JDK 17, an emulator plus one physical Android phone for gesture testing.
- Supabase CLI for local DB, migrations under `supabase/migrations/`.
- Repo already has the README, LICENSE, and `.gitignore`.

## Definition of Done

1. A student with a university email can sign in. Other domains are rejected.
2. They create a profile, join a competition solo or as a team, and swipe through that competition's deck.
3. Two mutual right swipes produce a match on both phones, with contact links visible only to the matched pair.
4. The match updates team membership correctly for SOLO↔SOLO and SOLO↔TEAM.
5. After the event ends, endorsements work and XP and badges appear on the profile.
6. RLS, matching, swipe cap, and XP each have passing SQL tests.

## Build order (riskiest first)

**Week 1 — Auth + schema**
- Confirm the university email domain.
- Supabase schema migrations for every table in SPEC §3, with constraints.
- Domain-restricted OTP sign-in working end to end on a phone. If the auth hook route fails, fall back to a `auth.users` insert trigger.
- RLS policies plus SQL tests.

**Week 2 — Swipe card prototype + app skeleton**
- Compose project: theme, navigation, Koin, Supabase client.
- Hand-rolled swipe card tuned on a physical device. This is the core feel of the app, so it gets real time.
- Profile create and edit screens.

**Week 3 — Competitions, listings, deck**
- Competition list, join as SOLO, create team plus TEAM listing.
- Deck query (active listings in the competition, not yet swiped, swipe rules from SPEC §3).
- Swipe insert wired to the deck, daily cap display.

**Week 4 — Matching**
- Matching trigger plus SQL tests.
- Match screen with contact handoff (WhatsApp / Telegram deep link).
- Matches list. Team membership update on match. Listing goes inactive when full.

**Week 5 — Gamification**
- XP triggers, badge awards, endorsement prompt after `eventEnd`.
- Reliability score on the card and profile.

**Week 6 — Polish + launch**
- Empty, loading, and error states. Accessibility pass (swipe buttons, content descriptions, TalkBack).
- Seed one real competition and 10+ users. Internal testing track or signed APK.
- Buffer. Do not launch on the deadline.

## Honest feasibility verdict

Achievable for one builder in ~6 weeks part-time if the scope holds. The biggest schedule risk is the swipe card feel, since gesture tuning eats time. Prototype it in week 2 on a real phone. The biggest product risk is cold start, not code. An empty deck means a dead app, so tie the launch to one real competition.

**Cut order if time compresses:** badges → endorsements and reliability score → TEAM listings (solo↔solo only) → XP.
**Irreducible core:** domain sign-in, profile, per-competition deck, mutual match with contact handoff.
