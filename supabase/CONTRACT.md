# Ether database contract

What the Android client can read, write, and call. Business rules live in the migrations; this file only describes the surface. All access is as the `authenticated` role. `anon` has no table or function access.

## Enums

| Type | Values |
|------|--------|
| `listing_kind` | `solo`, `team` |
| `swipe_direction` | `left`, `right` |
| `team_result` | `finalist`, `winner` |

## Tables

| Table | Client can | Notes |
|-------|-----------|-------|
| `profiles` | select all; update own `name, major, year, bio, skills, roles, avatar_url` | Row is created at sign-up. `xp` is read-only. |
| `profile_contacts` | select (RLS: see below); insert/update own `contact_url` | Kept apart from `profiles` so RLS can hide it. |
| `competitions` | select | Seeded by hand. |
| `teams` | select; insert with `owner_id = auth.uid()`; owner updates `name, pitch, open_roles` | Inserting a team adds the owner to `team_members` automatically. Fails with `registration_closed` after the competition's `registration_deadline`, and `already_in_team` if the owner is already in a team in that competition. |
| `team_members` | select | Written only by triggers (team creation, matches). Has a `competition_id` column (copied from the team); `(profile_id, competition_id)` is unique. |
| `listings` | select; insert own (`solo`: `profile_id = auth.uid()`, `team`: you own the team); update own `active` | Inserting fails with `registration_closed` after the deadline, and a `solo` insert fails with `already_in_team` (even with `active = false`). Activating fails with `registration_closed` / `already_in_team` / `team_full`. |
| `swipes` | select own | Written only through `swipe()`. |
| `matches` | select where you are on either side | Written only through `swipe()`. |
| `endorsements` | select given/received; insert with `from_profile_id = auth.uid()` | Fails with `not_teammates` / `event_not_ended`. Unique per (from, to, competition). |
| `badges`, `profile_badges`, `team_results`, `app_config` | select | Badges are awarded by triggers only. |
| `xp_events` | select own | XP ledger; `profiles.xp` is its sum. |

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

Raises `not_authenticated`, `competition_not_found`, `registration_closed` (after `registration_deadline`), `team_name_required` (blank name), or `already_in_team` (caller already in a team in that competition). Prefer this over inserting into `teams` and `listings` yourself.

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

### `my_matches(p_competition uuid) → setof record`

The caller's matches in one competition, newest first. Columns: `match_id, matched_at, my_listing_id`, then the other side's card (`listing_id, kind, profile_id, team_id, name, headline, skills, roles, open_roles, member_count, xp, reliability_score, avatar_url`), then `contact_url`. For a team, `contact_url` is the owner's contact.

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
| Profile complete: name, major, year, bio, skills, roles, contact | 50 | |
| First match (solo profile / team owner) | 25 | |
| Team formed or joined (team reaches 2 members, or you join one) | 100 per team | `first_team` |
| `finish_competition` (per competition) | 150 | `three_hackathons` at 3 |
| Positive endorsement received | 30 | `reliable` at 5 |
| Result confirmed by a teammate | 200 | `finalist` / `winner` |

Each award happens once per (profile, reason, reference), so retries never double-count.

## Sign-up

An `auth.users` trigger rejects any email whose domain is not exactly `app_config.allowed_email_domain` (case-insensitive, one `@`, no subdomains). It runs on sign-up and on any change to `email` or the pending `email_change`, so an email change to another domain also fails. The insert then creates the `profiles` row. The name comes from `user_metadata.name`, or the email's local part if that is missing. Change the domain with:

```sql
update app_config set value = 'your.domain' where key = 'allowed_email_domain';
```

A rejected sign-up reaches the client as a GoTrue 500, `"Database error saving new user"`. Show it as "use your university email". A rejected email change also surfaces as a GoTrue database error.

## Error messages

Errors raised from Postgres arrive as the PostgREST `message`. They are stable strings:

`not_authenticated`, `competition_not_found`, `team_name_required`, `listing_not_found`, `not_listing_owner`, `self_swipe`, `different_competition`, `listing_inactive`, `team_to_team`, `swipe_cap_reached`, `already_in_team`, `team_full`, `not_teammates`, `event_not_ended`, `not_team_member`, `result_already_confirmed`, `no_result_reported`, `cannot_confirm_own_result`, `email_domain_not_allowed`, `registration_closed`, `team_formed_after_deadline`.

## Local dev

`supabase start`, then `supabase db reset`. Demo logins are `demo1@student.example.ac.id` … `demo8@…` using email OTP. Read the 6-digit code in Mailpit at http://127.0.0.1:54324. Competition `Campus Hackathon 2026` has 6 solo listings (demo1–6) and team `Byte Club` (demo7 + demo8). `Data Challenge 2026` has ended, and its team `Night Owls` (demo1–3) can endorse each other.
