# Ether database contract

What the Android client can read, write, and call. Business rules live in the migrations; this file only describes the surface. All access is as the `authenticated` role. `anon` has no table or function access.

## Enums

| Type | Values |
|------|--------|
| `listing_kind` | `solo`, `team` |
| `swipe_direction` | `left`, `right` |
| `team_result` | `finalist`, `winner` |
| `hackathon_result` | `participant`, `honorable_mention`, `finalist`, `winner` (declared in rank order) |

## Tables

| Table | Client can | Notes |
|-------|-----------|-------|
| `profiles` | select all; update own `name, major, year, bio, skills, roles, avatar_url` | Row is created at sign-up. `xp` is read-only. |
| `profile_contacts` | select (RLS: see below); insert own `profile_id, phone, instagram, line_id`; update own `phone, instagram, line_id` | Kept apart from `profiles` so RLS can hide it. All three are nullable; see formats below. |
| `profile_hackathons` | select all; insert own `profile_id, name, year, result, project_name, url`; update own `name, year, result, project_name, url`; delete own | Past hackathons. Max 20 per profile (`hackathon_limit_reached`). See formats below. |
| `profile_projects` | select all; insert own `profile_id, title, blurb, url, tags`; update own `title, blurb, url, tags`; delete own | Projects. Max 20 per profile (`project_limit_reached`). |
| `competitions` | select | Seeded by hand. |
| `teams` | select; insert with `owner_id = auth.uid()`; owner updates `name, pitch, open_roles` | Inserting a team adds the owner to `team_members` automatically. Fails with `registration_closed` after the competition's `registration_deadline`, and `already_in_team` if the owner is already in a team in that competition. |
| `team_members` | select | Written only by triggers (team creation, matches). Has a `competition_id` column (copied from the team); `(profile_id, competition_id)` is unique. |
| `listings` | select; insert own (`solo`: `profile_id = auth.uid()`, `team`: you own the team); update own `active` | Inserting fails with `registration_closed` after the deadline, and a `solo` insert fails with `already_in_team` (even with `active = false`). Activating fails with `registration_closed` / `already_in_team` / `team_full`. |
| `swipes` | select own | Written only through `swipe()`. |
| `matches` | select where you are on either side | Written only through `swipe()`. |
| `endorsements` | select given/received; insert with `from_profile_id = auth.uid()` | Fails with `not_teammates` / `event_not_ended`. Unique per (from, to, competition). |
| `badges`, `profile_badges`, `team_results`, `app_config` | select | Badges are awarded by triggers only. |
| `xp_events` | select own | XP ledger; `profiles.xp` is its sum. |

**Contact formats.** Check constraints on `profile_contacts`; store values normalized, as the app does:

| Column | Format | Opens |
|--------|--------|-------|
| `phone` | `^\+?[0-9]{8,16}$`, country code included (the app writes digits only, `08…` → `628…`) | `https://wa.me/<phone>` |
| `instagram` | `^[A-Za-z0-9._]{1,30}$`, no `@` (the app lowercases) | `https://instagram.com/<instagram>` |
| `line_id` | `^[A-Za-z0-9._-]{4,20}$` (the app lowercases) | `https://line.me/ti/p/~<line_id>` |

A violation fails with SQLSTATE `23514`. Clear a field by writing `null`, not `''`.

**Track record formats.** Check constraints; a violation fails with SQLSTATE `23514`. Store trimmed values and write `null`, not `''`, for empty optional fields.

| Column | Format |
|--------|--------|
| `name`, `project_name`, `title` | 1–80 characters, no surrounding whitespace |
| `year` | 2000 to next year (`hackathon_year_out_of_range` above it, raised by a trigger because the bound moves) |
| `result` | `hackathon_result`, default `participant` |
| `blurb` | 1–140 characters, no surrounding whitespace |
| `url` | `https://` with a dotted host, no whitespace, at most 500 characters |
| `tags` | at most 6, each 1–30 characters, no surrounding whitespace |

`id` and `created_at` are not writable. Lists are read with `profile_id = <uid>`; the app orders hackathons by `year desc, created_at desc` and projects by `created_at desc`.

**Contact visibility.** A `profile_contacts` row is visible to its owner, to their teammates (any team they share), and to anyone on the other side of a match with them. A team listing's side means every current member of that team.

## RPCs

Call with `postgrest.rpc(name, params)`. Parameter names are exact.

### `swipe(p_from uuid, p_to uuid, p_direction swipe_direction) → jsonb`

```json
{"matched": true, "match_id": "uuid-or-null", "remaining": 17}
```

- The caller must own `p_from` (the solo's profile, or the team's owner).
- New swipes fail with `registration_closed` once the competition's `registration_deadline` has passed.
- Retrying an existing `(p_from, p_to)` swipe returns the current outcome without counting again, even if the listings have gone inactive since.
- A right swipe when the reverse right swipe exists creates the match. Team formation runs in the same transaction (see below).

### `create_team(p_competition uuid, p_name text, p_pitch text, p_open_roles text[]) → uuid`

Creates a team owned by the caller, adds the caller as its first member, and lists it as an active `team` listing, in one transaction. Returns the team id. The caller's solo listing in that competition, if any, goes inactive. `p_name` is trimmed; a blank `p_pitch` is stored as null.

Raises `hackathon_limit_reached`, `project_limit_reached`, `hackathon_year_out_of_range`, `not_authenticated`, `competition_not_found`, `registration_closed` (after `registration_deadline`), `team_name_required` (blank name), or `already_in_team` (caller already in a team in that competition). Prefer this over inserting into `teams` and `listings` yourself.

### `remaining_swipes() → int`

Swipes left today. The cap is 30 per profile per UTC day, across all competitions and listings.

### `deck(p_listing uuid, p_limit int default 20) → setof listing_cards`

Candidates for `p_listing`: same competition, active, not yet swiped by `p_listing`, not a listing you belong to (your solo listing or any team you are a member of), and team↔team excluded. Sorted newest first. `p_limit` is clamped to 0..100. Raises `not_listing_owner` if you don't own `p_listing`, and `listing_inactive` if `p_listing` is inactive.

Output columns (one row per card):

| Column | Type | Solo | Team |
|--------|------|------|------|
| `listing_id` | uuid | | |
| `competition_id` | uuid | | |
| `kind` | listing_kind | | |
| `profile_id` | uuid | profile | null |
| `team_id` | uuid | null | team |
| `owner_id` | uuid | profile | team owner |
| `name` | text | profile name | team name |
| `headline` | text | bio | pitch |
| `skills` | text[] | profile skills | distinct union of members' skills |
| `roles` | text[] | profile roles | distinct union of members' roles |
| `open_roles` | text[] | `{}` | team open roles |
| `member_count` | int | 1 | member count |
| `xp` | int | profile xp | sum of members' xp |
| `reliability_score` | int | profile score | sum of members' scores |
| `avatar_url` | text | profile avatar | owner's avatar |
| `active` | boolean | | |
| `created_at` | timestamptz | | |
| `best_result` | hackathon_result | profile's best | best across members |
| `best_hackathon` | text | that hackathon's name | |
| `best_hackathon_year` | int | that hackathon's year | |
| `hackathon_count` | int | profile's count | members' total |
| `project_count` | int | profile's count | members' total |

"Best" is the highest `result`, then the latest `year`, then the newest row. The three `best_*` columns are null when there are no hackathons.

### `my_matches(p_competition uuid) → setof record`

The caller's matches in one competition, newest first. Columns: `match_id, matched_at, my_listing_id`, then the other side's card (`listing_id, kind, profile_id, team_id, name, headline, skills, roles, open_roles, member_count, xp, reliability_score, avatar_url`), then `phone, instagram, line_id`. For a team, these are the owner's contacts. Any of them can be null.

### `reliability_score(p_profile uuid) → int`

Count of positive endorsements received.

### `report_result(p_team uuid, p_result team_result)` / `confirm_result(p_team uuid)`

After `event_end`, a member reports `finalist` or `winner`. A different member confirms it. Confirmation awards 200 XP plus the matching badge to every member. Once confirmed, the result is final: `report_result` raises `result_already_confirmed`. Both calls raise `team_formed_after_deadline` for a team created after the competition's `registration_deadline`.

### `finish_competition(p_competition uuid)` (admin only, not granted to clients)

Run from the SQL editor after the event. It awards "competition finished" XP to every team member and is idempotent.

## Team formation (automatic on match)

There is no accept step. A match is consent from both sides.

- **SOLO↔TEAM.** The solo is added to `team_members`.
- **SOLO↔SOLO.** A new team named `"<owner> & <other>"` is created. Its owner is the profile whose right swipe came first. Both people become members, and a `team` listing is created for it.
- Joining a team deactivates that profile's solo listing in the competition.
- When a team reaches `max_team_size`, its team listing is deactivated.
- A profile can be in only one team per competition (`already_in_team`). Concurrent matches for the same solo serialize: the later one fails with `listing_inactive`.

## Registration deadline

After a competition's `registration_deadline`, nothing new can form in it: listing and team inserts, listing activation, and new swipes all fail with `registration_closed`. Existing listings stay visible but cannot be swiped.

## XP and badges (SPEC §4)

| Trigger | XP | Badge |
|---------|----|-------|
| Profile complete: name, major, year, bio, skills, roles, and at least one of phone / instagram / line_id | 50 | |
| First match (solo profile / team owner) | 25 | |
| Team formed or joined (team reaches 2 members, or you join one) | 100 per team | `first_team` |
| `finish_competition` (per competition) | 150 | `three_hackathons` at 3 |
| Positive endorsement received | 30 | `reliable` at 5 |
| Result confirmed by a teammate | 200 | `finalist` / `winner` |

Each award happens once per (profile, reason, reference), so retries never double-count.

## Sign-up

Guests (the `anon` role) can read `competitions` and nothing else, so the app can be browsed before signing up.

Sign-up is open to any email (email and password, no confirmation step) and to Google accounts. An `auth.users` insert trigger creates the `profiles` row. The name comes from `user_metadata.name`, then `user_metadata.full_name` (Google), then the email's local part. The avatar comes from `user_metadata.avatar_url` or `user_metadata.picture` (Google).

## Error messages

Errors raised from Postgres arrive as the PostgREST `message`. They are stable strings:

`not_authenticated`, `competition_not_found`, `team_name_required`, `listing_not_found`, `not_listing_owner`, `self_swipe`, `different_competition`, `listing_inactive`, `team_to_team`, `swipe_cap_reached`, `already_in_team`, `team_full`, `not_teammates`, `event_not_ended`, `not_team_member`, `result_already_confirmed`, `no_result_reported`, `cannot_confirm_own_result`, `registration_closed`, `team_formed_after_deadline`.

## Local dev

`supabase start`, then `supabase db reset`. Demo logins are `demo1@student.example.ac.id` … `demo8@…` with the password `ether-demo` (local seed only). New accounts sign straight in. Competition `Campus Hackathon 2026` has 6 solo listings (demo1–6) and team `Byte Club` (demo7 + demo8). `Data Challenge 2026` has ended, and its team `Night Owls` (demo1–3) can endorse each other. Demo1, 2, 3, 5 and 8 have past hackathons and demo1, 3, 6 and 8 have projects, so deck cards show a track-record summary.
