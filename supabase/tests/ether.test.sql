begin;
create extension if not exists pgtap with schema extensions;
select no_plan();

-- ---------------------------------------------------------------- fixtures (as postgres)
-- Users A..G. A,B: solos who match. C: team owner. D: other team owner. E,F: solos joining C's team.
-- G: swipe-cap and deck user. Competition 1 is upcoming (max 3), competition 2 has ended.

create function pg_temp.login(p uuid) returns void language sql as $$
  select set_config('request.jwt.claims', json_build_object('sub', p, 'role', 'authenticated')::text, true);
  select set_config('role', 'authenticated', true);
$$;

select lives_ok(
  $$ insert into auth.users (id, email) values ('00000000-0000-0000-0000-0000000000ff', 'mallory@gmail.com') $$,
  'sign-up is open to any email domain');
select is(
  (select name from profiles where id = '00000000-0000-0000-0000-0000000000ff'), 'mallory',
  'an email sign-up is named after the local part');
select lives_ok(
  $$ insert into auth.users (id, email, raw_user_meta_data) values (
       '00000000-0000-0000-0000-0000000000fe', 'g.user@gmail.com',
       '{"full_name": "Gita Pratama", "avatar_url": "https://lh3.googleusercontent.com/a/x"}') $$,
  'a Google sign-up is accepted');
select results_eq(
  $$ select name, avatar_url from profiles where id = '00000000-0000-0000-0000-0000000000fe' $$,
  $$ values ('Gita Pratama'::text, 'https://lh3.googleusercontent.com/a/x'::text) $$,
  'a Google sign-up takes its name and photo from the Google profile');
select lives_ok(
  $$ update auth.users set email = 'mallory@external.example' where id = '00000000-0000-0000-0000-0000000000ff' $$,
  'changing email to any domain is allowed');

insert into auth.users (id, email)
select ('00000000-0000-0000-0000-0000000000' || x)::uuid, 'user' || x || '@student.example.ac.id'
from unnest(array['0a', '0b', '0c', '0d', '0e', '0f', '1a']) x;

select is((select name from profiles where id = '00000000-0000-0000-0000-00000000000a'), 'user0a',
  'allowed domain creates a profile named after the email');

insert into profile_contacts (profile_id, phone, instagram, line_id) values
  ('00000000-0000-0000-0000-00000000000a', '6281200000001', null, null),
  ('00000000-0000-0000-0000-00000000000b', '6281200000002', 'user.b', null),
  ('00000000-0000-0000-0000-00000000000c', null, null, 'user-c');

-- ---------------------------------------------------------------- contact formats

select throws_ok($$ update profile_contacts set phone = '0812-3456' where profile_id = '00000000-0000-0000-0000-00000000000a' $$,
  '23514', null, 'phone must be digits only');
select throws_ok($$ update profile_contacts set phone = '1234567' where profile_id = '00000000-0000-0000-0000-00000000000a' $$,
  '23514', null, 'phone needs at least 8 digits');
select throws_ok($$ update profile_contacts set phone = '12345678901234567' where profile_id = '00000000-0000-0000-0000-00000000000a' $$,
  '23514', null, 'phone allows at most 16 digits');
select lives_ok($$ update profile_contacts set phone = '+6281200000001' where profile_id = '00000000-0000-0000-0000-00000000000a' $$,
  'phone may carry a leading +');
update profile_contacts set phone = '6281200000001' where profile_id = '00000000-0000-0000-0000-00000000000a';
select throws_ok($$ update profile_contacts set instagram = '@user.a' where profile_id = '00000000-0000-0000-0000-00000000000a' $$,
  '23514', null, 'instagram is stored without the @');
select throws_ok($$ update profile_contacts set instagram = repeat('a', 31) where profile_id = '00000000-0000-0000-0000-00000000000a' $$,
  '23514', null, 'instagram allows at most 30 characters');
select throws_ok($$ update profile_contacts set instagram = '' where profile_id = '00000000-0000-0000-0000-00000000000a' $$,
  '23514', null, 'instagram cannot be empty (use null)');
select throws_ok($$ update profile_contacts set line_id = 'abc' where profile_id = '00000000-0000-0000-0000-00000000000a' $$,
  '23514', null, 'line_id needs at least 4 characters');
select throws_ok($$ update profile_contacts set line_id = 'has space' where profile_id = '00000000-0000-0000-0000-00000000000a' $$,
  '23514', null, 'line_id rejects spaces');
select hasnt_column('public', 'profile_contacts', 'contact_url', 'contact_url is gone');

insert into competitions (id, name, registration_deadline, event_end, min_team_size, max_team_size) values
  ('30000000-0000-0000-0000-000000000001', 'Upcoming', now() + interval '7 days', now() + interval '14 days', 2, 3),
  ('30000000-0000-0000-0000-000000000002', 'Ended',    now() - interval '14 days', now() - interval '7 days', 2, 3);

insert into teams (id, competition_id, owner_id, name) values
  ('20000000-0000-0000-0000-00000000000c', '30000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-00000000000c', 'Team C'),
  ('20000000-0000-0000-0000-00000000000d', '30000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-00000000000d', 'Team D');

insert into listings (id, competition_id, kind, profile_id, team_id) values
  ('10000000-0000-0000-0000-00000000000a', '30000000-0000-0000-0000-000000000001', 'solo', '00000000-0000-0000-0000-00000000000a', null),
  ('10000000-0000-0000-0000-00000000000b', '30000000-0000-0000-0000-000000000001', 'solo', '00000000-0000-0000-0000-00000000000b', null),
  ('10000000-0000-0000-0000-00000000000c', '30000000-0000-0000-0000-000000000001', 'team', null, '20000000-0000-0000-0000-00000000000c'),
  ('10000000-0000-0000-0000-00000000000d', '30000000-0000-0000-0000-000000000001', 'team', null, '20000000-0000-0000-0000-00000000000d'),
  ('10000000-0000-0000-0000-00000000000e', '30000000-0000-0000-0000-000000000001', 'solo', '00000000-0000-0000-0000-00000000000e', null),
  ('10000000-0000-0000-0000-00000000000f', '30000000-0000-0000-0000-000000000001', 'solo', '00000000-0000-0000-0000-00000000000f', null),
  ('10000000-0000-0000-0000-00000000001a', '30000000-0000-0000-0000-000000000001', 'solo', '00000000-0000-0000-0000-00000000001a', null);

-- ---------------------------------------------------------------- SOLO↔SOLO matching

select pg_temp.login('00000000-0000-0000-0000-00000000000a');

select is(
  (select count(*)::int from profile_contacts where profile_id = '00000000-0000-0000-0000-00000000000b'), 0,
  'contacts hidden before match');

select is(
  swipe('10000000-0000-0000-0000-00000000000a', '10000000-0000-0000-0000-00000000000b', 'right'),
  '{"matched": false, "match_id": null, "remaining": 29}'::jsonb,
  'one-sided right swipe does not match');
select is(remaining_swipes(), 29, 'remaining_swipes counts today''s swipes');

select throws_ok(
  $$ select swipe('10000000-0000-0000-0000-00000000000b', '10000000-0000-0000-0000-00000000000a', 'right') $$,
  '42501', 'not_listing_owner', 'cannot swipe from someone else''s listing');

select throws_ok(
  $$ update profiles set xp = 9999 where id = '00000000-0000-0000-0000-00000000000a' $$,
  '42501', null, 'client cannot update xp');
select throws_ok(
  $$ insert into matches (competition_id, listing_a, listing_b) values
     ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-00000000000a', '10000000-0000-0000-0000-00000000000e') $$,
  '42501', null, 'client cannot insert matches');
select throws_ok(
  $$ insert into profile_badges (profile_id, badge_code) values ('00000000-0000-0000-0000-00000000000a', 'winner') $$,
  '42501', null, 'client cannot award badges');

reset role;
select is((select count(*)::int from matches where competition_id = '30000000-0000-0000-0000-000000000001'), 0, 'no match after one-sided right');

select pg_temp.login('00000000-0000-0000-0000-00000000000b');
create temp table ab_result as
  select swipe('10000000-0000-0000-0000-00000000000b', '10000000-0000-0000-0000-00000000000a', 'right') r;
select is((select (r ->> 'matched')::boolean from ab_result), true, 'mutual right swipe matches');

select is(
  swipe('10000000-0000-0000-0000-00000000000b', '10000000-0000-0000-0000-00000000000a', 'right'),
  (select r from ab_result),
  'retrying the same swipe returns the same result');

reset role;
select is((select count(*)::int from matches where competition_id = '30000000-0000-0000-0000-000000000001'), 1, 'mutual right creates exactly one match');
select is((select count(*)::int from swipes where from_listing_id in
  ('10000000-0000-0000-0000-00000000000a', '10000000-0000-0000-0000-00000000000b')), 2,
  'retry did not insert a second swipe');

select is(
  (select owner_id from teams where id = (select team_id from team_members
     where profile_id = '00000000-0000-0000-0000-00000000000b')),
  '00000000-0000-0000-0000-00000000000a'::uuid,
  'SOLO↔SOLO: team owned by the earlier right-swiper');
select is(
  (select array_agg(profile_id order by profile_id) from team_members
   where team_id = (select team_id from team_members where profile_id = '00000000-0000-0000-0000-00000000000b')),
  array['00000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000b']::uuid[],
  'SOLO↔SOLO: both solos are members');
select is(
  (select array_agg(active order by id) from listings where id in
    ('10000000-0000-0000-0000-00000000000a', '10000000-0000-0000-0000-00000000000b')),
  array[false, false], 'SOLO↔SOLO: both solo listings inactive');
select is(
  (select active from listings l join team_members tm on tm.team_id = l.team_id
   where tm.profile_id = '00000000-0000-0000-0000-00000000000b'),
  true, 'SOLO↔SOLO: new team listing active below max size');

select is((select xp from profiles where id = '00000000-0000-0000-0000-00000000000a'), 125,
  'XP: first match 25 + team formed 100');
select is((select xp from profiles where id = '00000000-0000-0000-0000-00000000000b'), 125,
  'XP: both sides of a SOLO↔SOLO match get 125');
select is((select array_agg(badge_code) from profile_badges where profile_id = '00000000-0000-0000-0000-00000000000b'),
  array['first_team'], 'First Team badge awarded');

-- B joined a team owned by A, and never swiped its new team listing.
select pg_temp.login('00000000-0000-0000-0000-00000000000b');
select throws_ok(
  $$ select * from deck('10000000-0000-0000-0000-00000000000b') $$,
  'P0001', 'listing_inactive', 'deck rejects an inactive source listing');
reset role;
alter table listings disable trigger listing_active_guard;
update listings set active = true where id = '10000000-0000-0000-0000-00000000000b';
select pg_temp.login('00000000-0000-0000-0000-00000000000b');
select is(
  (select count(*)::int from deck('10000000-0000-0000-0000-00000000000b') where kind = 'team' and name = 'user0a & user0b'),
  0, 'deck never shows a team the caller is a member of');
reset role;
update listings set active = false where id = '10000000-0000-0000-0000-00000000000b';
alter table listings enable trigger listing_active_guard;

select pg_temp.login('00000000-0000-0000-0000-00000000000a');
select results_eq(
  $$ select phone, instagram, line_id from profile_contacts where profile_id = '00000000-0000-0000-0000-00000000000b' $$,
  $$ values ('6281200000002'::text, 'user.b'::text, null::text) $$,
  'contacts visible after match');
select results_eq(
  $$ select phone, instagram, line_id from my_matches('30000000-0000-0000-0000-000000000001') $$,
  $$ values ('6281200000002'::text, 'user.b'::text, null::text) $$,
  'my_matches returns the other side''s contacts');

reset role;
select pg_temp.login('00000000-0000-0000-0000-00000000000d');
select is(
  (select count(*)::int from profile_contacts where profile_id = '00000000-0000-0000-0000-00000000000a'), 0,
  'unmatched profile cannot read another user''s contacts');
select is((select count(*)::int from matches), 0, 'unmatched profile cannot read other matches');

-- ---------------------------------------------------------------- one team per competition, listing creation

reset role;
select throws_ok(
  $$ insert into team_members (team_id, profile_id) values
     ('20000000-0000-0000-0000-00000000000d', '00000000-0000-0000-0000-00000000000a') $$,
  'P0001', 'already_in_team', 'profile cannot join a second team in the same competition');
select col_is_unique('public', 'team_members', array['profile_id', 'competition_id'],
  'one team per profile per competition is a unique constraint (concurrency backstop)');

select pg_temp.login('00000000-0000-0000-0000-00000000000d');
select throws_ok(
  $$ insert into listings (competition_id, kind, profile_id, active) values
     ('30000000-0000-0000-0000-000000000001', 'solo', '00000000-0000-0000-0000-00000000000d', false) $$,
  'P0001', 'already_in_team', 'team member cannot create a solo listing, even inactive');

-- ---------------------------------------------------------------- registration deadline

select throws_ok(
  $$ insert into listings (competition_id, kind, profile_id) values
     ('30000000-0000-0000-0000-000000000002', 'solo', '00000000-0000-0000-0000-00000000000d') $$,
  'P0001', 'registration_closed', 'listing rejected after registration_deadline');
select throws_ok(
  $$ insert into teams (competition_id, owner_id, name) values
     ('30000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-00000000000d', 'Late') $$,
  'P0001', 'registration_closed', 'team rejected after registration_deadline');
reset role;

-- Listings left active from before the deadline (fixture inserts with a past created_at).
insert into listings (id, competition_id, kind, profile_id, active, created_at) values
  ('10000000-0000-0000-0000-0000000000e1', '30000000-0000-0000-0000-000000000002', 'solo', '00000000-0000-0000-0000-00000000000e', false, now() - interval '20 days'),
  ('10000000-0000-0000-0000-0000000000e2', '30000000-0000-0000-0000-000000000002', 'solo', '00000000-0000-0000-0000-00000000000f', false, now() - interval '20 days');
alter table listings disable trigger listing_active_guard;
update listings set active = true where id in ('10000000-0000-0000-0000-0000000000e1', '10000000-0000-0000-0000-0000000000e2');
alter table listings enable trigger listing_active_guard;

select pg_temp.login('00000000-0000-0000-0000-00000000000e');
select throws_ok(
  $$ select swipe('10000000-0000-0000-0000-0000000000e1', '10000000-0000-0000-0000-0000000000e2', 'right') $$,
  'P0001', 'registration_closed', 'swipe rejected after registration_deadline');
select throws_ok(
  $$ update listings set active = true where id = '10000000-0000-0000-0000-0000000000e1' $$,
  'P0001', 'registration_closed', 'listing cannot be reactivated after registration_deadline');
reset role;
select pg_temp.login('00000000-0000-0000-0000-00000000000d');

-- ---------------------------------------------------------------- TEAM rules and SOLO↔TEAM

select throws_ok(
  $$ select swipe('10000000-0000-0000-0000-00000000000d', '10000000-0000-0000-0000-00000000000c', 'right') $$,
  'P0001', 'team_to_team', 'team→team swipe rejected');

reset role;
select pg_temp.login('00000000-0000-0000-0000-00000000000e');
select is(
  swipe('10000000-0000-0000-0000-00000000000e', '10000000-0000-0000-0000-00000000000c', 'right') ->> 'matched',
  'false', 'solo→team right waits for the owner');
reset role;
select pg_temp.login('00000000-0000-0000-0000-00000000000c');
select is(
  swipe('10000000-0000-0000-0000-00000000000c', '10000000-0000-0000-0000-00000000000e', 'right') ->> 'matched',
  'true', 'team owner right back matches');
reset role;

select is(
  (select count(*)::int from team_members where team_id = '20000000-0000-0000-0000-00000000000c'), 2,
  'SOLO↔TEAM: solo joined the team');
select is((select active from listings where id = '10000000-0000-0000-0000-00000000000e'), false,
  'SOLO↔TEAM: solo listing inactive');
select is((select active from listings where id = '10000000-0000-0000-0000-00000000000c'), true,
  'team listing stays active below max size');

select pg_temp.login('00000000-0000-0000-0000-00000000000e');
select results_eq(
  $$ select phone, instagram, line_id from my_matches('30000000-0000-0000-0000-000000000001') $$,
  $$ values (null::text, null::text, 'user-c'::text) $$,
  'solo sees the team owner''s contacts');
reset role;

select pg_temp.login('00000000-0000-0000-0000-00000000000f');
select swipe('10000000-0000-0000-0000-00000000000f', '10000000-0000-0000-0000-00000000000c', 'right');
reset role;
select pg_temp.login('00000000-0000-0000-0000-00000000000c');
select swipe('10000000-0000-0000-0000-00000000000c', '10000000-0000-0000-0000-00000000000f', 'right');
reset role;
select is((select active from listings where id = '10000000-0000-0000-0000-00000000000c'), false,
  'team listing inactive once it reaches max_team_size');

select pg_temp.login('00000000-0000-0000-0000-00000000000f');
select is((select line_id from profile_contacts where profile_id = '00000000-0000-0000-0000-00000000000c'),
  'user-c', 'matched solo reads team owner contact via RLS');
reset role;

-- ---------------------------------------------------------------- deck and swipe cap (user G)

select pg_temp.login('00000000-0000-0000-0000-00000000001a');
select is(
  (select array_agg(name order by name) from deck('10000000-0000-0000-0000-00000000001a')),
  array['Team D', 'user0a & user0b'],
  'deck: active listings in the competition except self');
select swipe('10000000-0000-0000-0000-00000000001a', '10000000-0000-0000-0000-00000000000d', 'left');
select is(
  (select array_agg(name) from deck('10000000-0000-0000-0000-00000000001a')),
  array['user0a & user0b'], 'deck: already swiped listings are excluded');
reset role;

-- 29 more swipes today for G (fixture insert bypasses the RPC; only the count matters).
insert into swipes (from_listing_id, to_listing_id, direction, swiped_by, created_at)
select f, t, 'left', '00000000-0000-0000-0000-00000000001a', now()
from (select a.id f, b.id t from listings a cross join listings b
      where a.id <> b.id and '10000000-0000-0000-0000-00000000001a' not in (a.id, b.id)
        and not exists (select 1 from swipes s where s.from_listing_id = a.id and s.to_listing_id = b.id)
      limit 29) pairs;

select pg_temp.login('00000000-0000-0000-0000-00000000001a');
select is(remaining_swipes(), 0, 'cap: 30 swipes used');
select throws_ok(
  $$ select swipe('10000000-0000-0000-0000-00000000001a',
       (select listing_id from deck('10000000-0000-0000-0000-00000000001a') limit 1), 'right') $$,
  'P0001', 'swipe_cap_reached', '31st swipe of the day rejected');
reset role;

-- ---------------------------------------------------------------- endorsements, finish, results

insert into teams (id, competition_id, owner_id, name, created_at) values
  ('20000000-0000-0000-0000-00000000000e', '30000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-00000000000a', 'Done', now() - interval '20 days');
insert into team_members (team_id, profile_id) values
  ('20000000-0000-0000-0000-00000000000e', '00000000-0000-0000-0000-00000000000b');

select pg_temp.login('00000000-0000-0000-0000-00000000000a');
select throws_ok(
  $$ insert into endorsements (from_profile_id, to_profile_id, competition_id, positive) values
     ('00000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000b', '30000000-0000-0000-0000-000000000001', true) $$,
  'P0001', 'event_not_ended', 'endorsement rejected before event_end');
select throws_ok(
  $$ insert into endorsements (from_profile_id, to_profile_id, competition_id, positive) values
     ('00000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000e', '30000000-0000-0000-0000-000000000002', true) $$,
  'P0001', 'not_teammates', 'endorsement rejected between non-teammates');
select lives_ok(
  $$ insert into endorsements (from_profile_id, to_profile_id, competition_id, positive) values
     ('00000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000b', '30000000-0000-0000-0000-000000000002', true) $$,
  'teammate endorsement after event_end accepted');
select is(reliability_score('00000000-0000-0000-0000-00000000000b'), 1, 'reliability score counts positive endorsements');

select lives_ok($$ select report_result('20000000-0000-0000-0000-00000000000e', 'winner') $$, 'member reports result');
select throws_ok($$ select confirm_result('20000000-0000-0000-0000-00000000000e') $$,
  'P0001', 'cannot_confirm_own_result', 'reporter cannot confirm own result');
reset role;
select pg_temp.login('00000000-0000-0000-0000-00000000000b');
select lives_ok($$ select confirm_result('20000000-0000-0000-0000-00000000000e') $$, 'teammate confirms result');
select throws_ok($$ select report_result('20000000-0000-0000-0000-00000000000e', 'finalist') $$,
  'P0001', 'result_already_confirmed', 'confirmed result cannot be re-reported');
reset role;
select is((select result from team_results where team_id = '20000000-0000-0000-0000-00000000000e'), 'winner'::team_result,
  'confirmed result is unchanged');

-- A team formed after the deadline (admin moved the deadline earlier) cannot claim a result.
insert into teams (id, competition_id, owner_id, name, created_at) values
  ('20000000-0000-0000-0000-0000000000e3', '30000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-00000000000c', 'Late', now() - interval '20 days');
update teams set created_at = now() - interval '10 days' where id = '20000000-0000-0000-0000-0000000000e3';
select pg_temp.login('00000000-0000-0000-0000-00000000000c');
select throws_ok($$ select report_result('20000000-0000-0000-0000-0000000000e3', 'winner') $$,
  'P0001', 'team_formed_after_deadline', 'team formed after registration_deadline cannot report a result');
reset role;

select finish_competition('30000000-0000-0000-0000-000000000002');

-- B: 125 (match + team C1) + 100 (team in comp 2) + 30 (endorsement) + 200 (result) + 150 (finished)
select is((select xp from profiles where id = '00000000-0000-0000-0000-00000000000b'), 605,
  'XP totals across all triggers');
select is((select array_agg(badge_code order by badge_code) from profile_badges where profile_id = '00000000-0000-0000-0000-00000000000b'),
  array['first_team', 'winner'], 'Winner badge after confirmed result');
select finish_competition('30000000-0000-0000-0000-000000000002');
select is((select xp from profiles where id = '00000000-0000-0000-0000-00000000000b'), 605,
  'finish_competition is idempotent');

-- ---------------------------------------------------------------- profile completion

select pg_temp.login('00000000-0000-0000-0000-00000000000d');
insert into profile_contacts (profile_id, instagram) values ('00000000-0000-0000-0000-00000000000d', 'user.d');
update profiles set major = 'CS', year = 2, bio = 'hi', skills = '{Go}', roles = '{Backend}'
where id = '00000000-0000-0000-0000-00000000000d';
select throws_ok(
  $$ insert into profile_contacts (profile_id, phone) values ('00000000-0000-0000-0000-00000000000e', '6281200000009') $$,
  '42501', null, 'cannot write another user''s contacts');
reset role;
select is((select xp from profiles where id = '00000000-0000-0000-0000-00000000000d'), 50,
  'profile completed with only an Instagram handle awards 50 XP');

-- A contacts row with every field empty does not complete a profile; adding a LINE ID does.
select pg_temp.login('00000000-0000-0000-0000-0000000000ff');
insert into profile_contacts (profile_id) values ('00000000-0000-0000-0000-0000000000ff');
update profiles set major = 'CS', year = 2, bio = 'hi', skills = '{Go}', roles = '{Backend}'
where id = '00000000-0000-0000-0000-0000000000ff';
reset role;
select is((select xp from profiles where id = '00000000-0000-0000-0000-0000000000ff'), 0,
  'profile without any contact is not complete');
select pg_temp.login('00000000-0000-0000-0000-0000000000ff');
update profile_contacts set line_id = 'mallory.l' where profile_id = '00000000-0000-0000-0000-0000000000ff';
reset role;
select is((select xp from profiles where id = '00000000-0000-0000-0000-0000000000ff'), 50,
  'adding a LINE ID completes the profile');

-- ---------------------------------------------------------------- create_team RPC

insert into competitions (id, name, registration_deadline, event_end, min_team_size, max_team_size) values
  ('30000000-0000-0000-0000-000000000003', 'Third', now() + interval '7 days', now() + interval '14 days', 2, 3);
insert into listings (id, competition_id, kind, profile_id) values
  ('10000000-0000-0000-0000-0000000000f3', '30000000-0000-0000-0000-000000000003', 'solo', '00000000-0000-0000-0000-00000000000c');

select pg_temp.login('00000000-0000-0000-0000-00000000000d');
select is((select count(*)::int from profile_contacts where profile_id = '00000000-0000-0000-0000-00000000000c'), 0,
  'contact hidden before becoming teammates');
reset role;

select pg_temp.login('00000000-0000-0000-0000-00000000000c');
create temp table c3 as
  select create_team('30000000-0000-0000-0000-000000000003', ' Rocket ', 'We ship', array['Mobile']) id;
reset role;
select is((select array[name, pitch, owner_id::text] from teams where id = (select id from c3)),
  array['Rocket', 'We ship', '00000000-0000-0000-0000-00000000000c'], 'create_team: team owned by the caller, name trimmed');
select is((select array_agg(profile_id) from team_members where team_id = (select id from c3)),
  array['00000000-0000-0000-0000-00000000000c']::uuid[], 'create_team: owner is the only member');
select is((select active from listings where team_id = (select id from c3) and kind = 'team'), true,
  'create_team: active TEAM listing created');
select is((select active from listings where id = '10000000-0000-0000-0000-0000000000f3'), false,
  'create_team: the owner''s solo listing goes inactive');

select pg_temp.login('00000000-0000-0000-0000-00000000000c');
select throws_ok($$ select create_team('30000000-0000-0000-0000-000000000003', 'Again', null, '{}') $$,
  'P0001', 'already_in_team', 'create_team: second team in the same competition rejected');
select throws_ok($$ select create_team('30000000-0000-0000-0000-000000000002', 'Late', null, '{}') $$,
  'P0001', 'registration_closed', 'create_team: rejected after registration_deadline');
select throws_ok($$ select create_team('30000000-0000-0000-0000-0000000000ff', 'Nowhere', null, '{}') $$,
  'P0001', 'competition_not_found', 'create_team: unknown competition rejected');
reset role;
select pg_temp.login('00000000-0000-0000-0000-00000000000d');
select throws_ok($$ select create_team('30000000-0000-0000-0000-000000000003', '  ', null, '{}') $$,
  'P0001', 'team_name_required', 'create_team: blank name rejected');
reset role;
select is((select count(*)::int from teams where competition_id = '30000000-0000-0000-0000-000000000003'), 1,
  'create_team: failed calls leave no team behind');

select set_config('request.jwt.claims', '', true);
select throws_ok($$ select create_team('30000000-0000-0000-0000-000000000003', 'Anon', null, '{}') $$,
  '42501', 'not_authenticated', 'create_team: requires a signed-in user');

insert into team_members (team_id, profile_id) values ((select id from c3), '00000000-0000-0000-0000-00000000000d');
select pg_temp.login('00000000-0000-0000-0000-00000000000d');
select is((select line_id from profile_contacts where profile_id = '00000000-0000-0000-0000-00000000000c'),
  'user-c', 'teammates can read each other''s contact without a match');
reset role;

-- ---------------------------------------------------------------- track record (past hackathons, projects)
-- A owns the rows. D is another signed-in user. Counts are scoped to fixture profiles.

select pg_temp.login('00000000-0000-0000-0000-00000000000a');
select lives_ok(
  $$ insert into profile_hackathons (profile_id, name, year, result, project_name, url) values
     ('00000000-0000-0000-0000-00000000000a', 'GEMASTIK', 2025, 'finalist', 'BusKu', 'https://github.com/example/busku') $$,
  'owner adds a past hackathon');
select lives_ok(
  $$ insert into profile_projects (profile_id, title, blurb, url, tags) values
     ('00000000-0000-0000-0000-00000000000a', 'BusKu', 'Predicts campus bus arrivals.', null, '{Kotlin,PostgreSQL}') $$,
  'owner adds a project');
select throws_ok(
  $$ insert into profile_projects (id, profile_id, title, blurb) values
     ('40000000-0000-0000-0000-0000000000ff', '00000000-0000-0000-0000-00000000000a', 'Mine', 'x') $$,
  '42501', null, 'id is not writable by clients');
reset role;
-- Pin the ids the rest of this section refers to.
update profile_hackathons set id = '40000000-0000-0000-0000-00000000000a' where profile_id = '00000000-0000-0000-0000-00000000000a';
update profile_projects set id = '40000000-0000-0000-0000-0000000000aa' where profile_id = '00000000-0000-0000-0000-00000000000a';
select pg_temp.login('00000000-0000-0000-0000-00000000000a');
select lives_ok(
  $$ update profile_hackathons set result = 'winner', project_name = null where id = '40000000-0000-0000-0000-00000000000a' $$,
  'owner edits a past hackathon');
select results_eq(
  $$ select result::text, project_name from profile_hackathons where id = '40000000-0000-0000-0000-00000000000a' $$,
  $$ values ('winner'::text, null::text) $$,
  'the edit is saved');
select lives_ok(
  $$ update profile_projects set blurb = 'Bus ETAs from GPS pings.', tags = '{Kotlin}' where id = '40000000-0000-0000-0000-0000000000aa' $$,
  'owner edits a project');
select is((select blurb from profile_projects where id = '40000000-0000-0000-0000-0000000000aa'),
  'Bus ETAs from GPS pings.', 'the project edit is saved');
select throws_ok(
  $$ insert into profile_projects (profile_id, title, blurb, created_at) values
     ('00000000-0000-0000-0000-00000000000a', 'Backdated', 'x', now() - interval '1 year') $$,
  '42501', null, 'created_at is not writable by clients');
reset role;

select pg_temp.login('00000000-0000-0000-0000-00000000000d');
select is((select count(*)::int from profile_hackathons where profile_id = '00000000-0000-0000-0000-00000000000a'), 1,
  'other signed-in users can read someone''s hackathons');
select is((select count(*)::int from profile_projects where profile_id = '00000000-0000-0000-0000-00000000000a'), 1,
  'other signed-in users can read someone''s projects');
select throws_ok(
  $$ insert into profile_hackathons (profile_id, name, year) values ('00000000-0000-0000-0000-00000000000a', 'Fake', 2025) $$,
  '42501', null, 'cannot add a hackathon to someone else''s profile');
select throws_ok(
  $$ insert into profile_projects (profile_id, title, blurb) values ('00000000-0000-0000-0000-00000000000a', 'Fake', 'x') $$,
  '42501', null, 'cannot add a project to someone else''s profile');
update profile_hackathons set name = 'Hacked' where id = '40000000-0000-0000-0000-00000000000a';
delete from profile_hackathons where id = '40000000-0000-0000-0000-00000000000a';
update profile_projects set title = 'Hacked' where id = '40000000-0000-0000-0000-0000000000aa';
delete from profile_projects where id = '40000000-0000-0000-0000-0000000000aa';
select throws_ok(
  $$ update profile_hackathons set profile_id = '00000000-0000-0000-0000-00000000000d' where id = '40000000-0000-0000-0000-00000000000a' $$,
  '42501', null, 'profile_id cannot be reassigned');
reset role;
select results_eq(
  $$ select h.name, p.title from profile_hackathons h, profile_projects p
     where h.id = '40000000-0000-0000-0000-00000000000a' and p.id = '40000000-0000-0000-0000-0000000000aa' $$,
  $$ values ('GEMASTIK'::text, 'BusKu'::text) $$,
  'another user''s update and delete touch nothing');

-- Formats and bounds (as the owner, so RLS passes and only the checks can fail).
select pg_temp.login('00000000-0000-0000-0000-00000000000a');
select throws_ok($$ insert into profile_hackathons (profile_id, name, year) values ('00000000-0000-0000-0000-00000000000a', 'Old', 1999) $$,
  '23514', null, 'year before 2000 rejected');
select throws_ok(
  $$ insert into profile_hackathons (profile_id, name, year) values
     ('00000000-0000-0000-0000-00000000000a', 'Future', extract(year from now())::int + 2) $$,
  '23514', 'hackathon_year_out_of_range', 'year after next year rejected');
select throws_ok(
  $$ update profile_hackathons set year = extract(year from now())::int + 2 where id = '40000000-0000-0000-0000-00000000000a' $$,
  '23514', 'hackathon_year_out_of_range', 'year bound also holds on edit');
select lives_ok(
  $$ insert into profile_hackathons (profile_id, name, year) values
     ('00000000-0000-0000-0000-00000000000a', 'Next Year', extract(year from now())::int + 1) $$,
  'next year is allowed (registered but not held yet)');
select is((select result::text from profile_hackathons where profile_id = '00000000-0000-0000-0000-00000000000a' and name = 'Next Year'), 'participant',
  'result defaults to participant');
select throws_ok($$ insert into profile_hackathons (profile_id, name, year) values ('00000000-0000-0000-0000-00000000000a', '   ', 2025) $$,
  '23514', null, 'blank hackathon name rejected');
select throws_ok($$ insert into profile_hackathons (profile_id, name, year) values ('00000000-0000-0000-0000-00000000000a', repeat('x', 81), 2025) $$,
  '23514', null, 'hackathon name allows at most 80 characters');
select throws_ok($$ insert into profile_hackathons (profile_id, name, year, project_name) values ('00000000-0000-0000-0000-00000000000a', 'X', 2025, '') $$,
  '23514', null, 'empty project_name rejected (use null)');
select throws_ok($$ insert into profile_hackathons (profile_id, name, year, result) values ('00000000-0000-0000-0000-00000000000a', 'X', 2025, 'champion') $$,
  '22P02', null, 'result must be a hackathon_result');
select throws_ok($$ insert into profile_hackathons (profile_id, name, year, url) values ('00000000-0000-0000-0000-00000000000a', 'X', 2025, 'http://example.com') $$,
  '23514', null, 'http links rejected');
select throws_ok($$ insert into profile_hackathons (profile_id, name, year, url) values ('00000000-0000-0000-0000-00000000000a', 'X', 2025, 'https://') $$,
  '23514', null, 'a link needs a host');
select throws_ok($$ insert into profile_projects (profile_id, title, blurb, url) values ('00000000-0000-0000-0000-00000000000a', 'X', 'x', 'javascript:alert(1)') $$,
  '23514', null, 'project links must be https');
select throws_ok($$ insert into profile_projects (profile_id, title, blurb, url) values ('00000000-0000-0000-0000-00000000000a', 'X', 'x', 'https://exa mple.com') $$,
  '23514', null, 'links cannot contain spaces');
select throws_ok($$ insert into profile_projects (profile_id, title, blurb) values ('00000000-0000-0000-0000-00000000000a', 'X', repeat('x', 141)) $$,
  '23514', null, 'blurb allows at most 140 characters');
select lives_ok($$ insert into profile_projects (profile_id, title, blurb) values ('00000000-0000-0000-0000-00000000000a', 'X', repeat('x', 140)) $$,
  'blurb of exactly 140 characters accepted');
select throws_ok($$ insert into profile_projects (profile_id, title, blurb) values ('00000000-0000-0000-0000-00000000000a', 'X', ' ') $$,
  '23514', null, 'blank blurb rejected');
select throws_ok($$ insert into profile_projects (profile_id, title, blurb, tags) values ('00000000-0000-0000-0000-00000000000a', 'X', 'x', '{a,b,c,d,e,f,g}') $$,
  '23514', null, 'at most 6 tags');
select throws_ok($$ insert into profile_projects (profile_id, title, blurb, tags) values ('00000000-0000-0000-0000-00000000000a', 'X', 'x', '{Kotlin,""}') $$,
  '23514', null, 'empty tags rejected');
select throws_ok($$ insert into profile_projects (profile_id, title, blurb, tags) values ('00000000-0000-0000-0000-00000000000a', 'X', 'x', array[repeat('t', 31)]) $$,
  '23514', null, 'tags allow at most 30 characters');
reset role;

-- Deck summary. G's deck holds the A & B team; its card sums both members' track records.
insert into profile_hackathons (profile_id, name, year, result) values
  ('00000000-0000-0000-0000-00000000000b', 'Data Cup', 2026, 'finalist'),
  ('00000000-0000-0000-0000-00000000000b', 'Old Cup', 2023, 'winner');
insert into profile_projects (profile_id, title, blurb) values ('00000000-0000-0000-0000-00000000000b', 'Proj B', 'x');
select pg_temp.login('00000000-0000-0000-0000-00000000001a');
select results_eq(
  $$ select best_result::text, best_hackathon, best_hackathon_year, hackathon_count, project_count
     from deck('10000000-0000-0000-0000-00000000001a') where name = 'user0a & user0b' $$,
  $$ values ('winner'::text, 'GEMASTIK'::text, 2025, 4, 3) $$,
  'team card: best result across members (latest winner first), with hackathon and project counts');
reset role;
select results_eq(
  $$ select best_result, best_hackathon, hackathon_count, project_count from listing_cards
     where listing_id = '10000000-0000-0000-0000-00000000001a' $$,
  $$ values (null::hackathon_result, null::text, 0, 0) $$,
  'solo card without a track record has an empty summary');
insert into profile_projects (profile_id, title, blurb) values ('00000000-0000-0000-0000-00000000001a', 'G1', 'x');
select is((select project_count from listing_cards where listing_id = '10000000-0000-0000-0000-00000000001a'), 1,
  'solo card counts the profile''s projects');

-- Cap: 20 of each per profile. A already has 2 hackathons and 2 projects.
select pg_temp.login('00000000-0000-0000-0000-00000000000a');
select lives_ok(
  $$ insert into profile_hackathons (profile_id, name, year)
     select '00000000-0000-0000-0000-00000000000a', 'H' || n, 2024 from generate_series(1, 18) n $$,
  'up to 20 hackathons per profile');
select throws_ok(
  $$ insert into profile_hackathons (profile_id, name, year) values ('00000000-0000-0000-0000-00000000000a', 'H21', 2024) $$,
  'P0001', 'hackathon_limit_reached', 'the 21st hackathon is rejected');
select lives_ok(
  $$ insert into profile_projects (profile_id, title, blurb)
     select '00000000-0000-0000-0000-00000000000a', 'P' || n, 'x' from generate_series(1, 18) n $$,
  'up to 20 projects per profile');
select throws_ok(
  $$ insert into profile_projects (profile_id, title, blurb) values ('00000000-0000-0000-0000-00000000000a', 'P21', 'x') $$,
  'P0001', 'project_limit_reached', 'the 21st project is rejected');
select lives_ok($$ delete from profile_hackathons where id = '40000000-0000-0000-0000-00000000000a' $$, 'owner deletes a hackathon');
select lives_ok($$ delete from profile_projects where id = '40000000-0000-0000-0000-0000000000aa' $$, 'owner deletes a project');
select is((select count(*)::int from profile_hackathons where id = '40000000-0000-0000-0000-00000000000a'), 0, 'the hackathon is gone');
select is((select count(*)::int from profile_projects where id = '40000000-0000-0000-0000-0000000000aa'), 0, 'the project is gone');
select lives_ok(
  $$ insert into profile_hackathons (profile_id, name, year) values ('00000000-0000-0000-0000-00000000000a', 'Back under', 2024) $$,
  'deleting frees a slot under the cap');
reset role;

-- ---------------------------------------------------------------- competition details
-- Fixtures 30000000-…-0000000000d1..d3 only; the hosted DB has real competitions, so every count is
-- scoped to these ids.
select has_column('public', 'competitions', 'organizer', 'competitions have an organizer');
select col_type_is('public', 'competitions', 'mode', 'competition_mode', 'mode is the competition_mode enum');
select col_default_is('public', 'competitions', 'mode', 'online', 'mode defaults to online');
select enum_has_labels('public', 'competition_mode', array['online', 'offline', 'hybrid'], 'competition_mode values');

insert into competitions (id, name, url, registration_deadline, event_end, min_team_size, max_team_size,
                          organizer, prize, mode, city, tags) values
  ('30000000-0000-0000-0000-0000000000d1', 'Details Offline', 'https://dorahacks.io/hackathon/even-realities/detail',
   now() + interval '7 days', now() + interval '14 days', 2, 4,
   'Test Org', 'Rp 50.000.000', 'offline', 'Yogyakarta', '{AI,Mobile}');
insert into competitions (id, name, registration_deadline, event_end, min_team_size, max_team_size) values
  ('30000000-0000-0000-0000-0000000000d2', 'Details Defaults', now() + interval '7 days', now() + interval '14 days', 2, 4);
select results_eq(
  $$ select mode::text, tags, organizer, prize, city from competitions where id = '30000000-0000-0000-0000-0000000000d2' $$,
  $$ values ('online'::text, '{}'::text[], null::text, null::text, null::text) $$,
  'a competition without details is online with no tags');
select results_eq(
  $$ select mode::text, city, tags from competitions where id = '30000000-0000-0000-0000-0000000000d1' $$,
  $$ values ('offline'::text, 'Yogyakarta'::text, '{AI,Mobile}'::text[]) $$,
  'details are stored as written');

select lives_ok(
  $$ update competitions set mode = 'hybrid', prize = '$10,000 + swag', tags = '{a,b,c,d,e,f,g,h}'
     where id = '30000000-0000-0000-0000-0000000000d1' $$,
  'hybrid with a city, free-text prize and 8 tags is accepted');
select throws_ok(
  $$ update competitions set tags = '{a,b,c,d,e,f,g,h,i}' where id = '30000000-0000-0000-0000-0000000000d1' $$,
  '23514', null, 'at most 8 tags');
select throws_ok(
  $$ update competitions set tags = '{AI,ai}' where id = '30000000-0000-0000-0000-0000000000d1' $$,
  '23514', null, 'tags are unique ignoring case');
select throws_ok(
  $$ update competitions set tags = '{" AI"}' where id = '30000000-0000-0000-0000-0000000000d1' $$,
  '23514', null, 'tags have no surrounding whitespace');
select throws_ok(
  $$ update competitions set tags = '{""}' where id = '30000000-0000-0000-0000-0000000000d1' $$,
  '23514', null, 'tags are not empty');
select throws_ok(
  $$ update competitions set mode = 'online' where id = '30000000-0000-0000-0000-0000000000d1' $$,
  '23514', null, 'an online competition has no city');
select lives_ok(
  $$ update competitions set mode = 'online', city = null where id = '30000000-0000-0000-0000-0000000000d1' $$,
  'going online clears the city');
select throws_ok(
  $$ update competitions set organizer = '' where id = '30000000-0000-0000-0000-0000000000d1' $$,
  '23514', null, 'organizer is null, not blank');
select throws_ok(
  $$ update competitions set prize = ' Rp 1 ' where id = '30000000-0000-0000-0000-0000000000d1' $$,
  '23514', null, 'prize is trimmed');
select throws_ok(
  $$ update competitions set url = 'http://example.com/hack' where id = '30000000-0000-0000-0000-0000000000d1' $$,
  '23514', null, 'the official page must be https');
select throws_ok(
  $$ update competitions set url = 'https://localhost/hack' where id = '30000000-0000-0000-0000-0000000000d1' $$,
  '23514', null, 'the official page needs a dotted host');
select throws_ok(
  $$ insert into competitions (id, name, registration_deadline, event_end, max_team_size, mode)
     values ('30000000-0000-0000-0000-0000000000d3', 'Bad mode', now(), now(), 3, 'metaverse') $$,
  '22P02', null, 'mode is a closed set');

select pg_temp.login('00000000-0000-0000-0000-00000000000a');
select throws_ok(
  $$ update competitions set prize = 'free' where id = '30000000-0000-0000-0000-0000000000d1' $$,
  '42501', null, 'signed-in users cannot edit competitions');
reset role;


-- ---------------------------------------------------------------- avatar faces
select pg_temp.login('00000000-0000-0000-0000-00000000000a');
select lives_ok(
  $$ update profiles set avatar_face = 'citra' where id = '00000000-0000-0000-0000-00000000000a' $$,
  'a user can pick a face for their picture');
select throws_ok(
  $$ update profiles set avatar_face = 'nobody' where id = '00000000-0000-0000-0000-00000000000a' $$,
  '22P02', null, 'only bundled faces can be picked');
select is(
  (select count(*)::int from profiles where id = '00000000-0000-0000-0000-00000000000b' and avatar_face is not null), 0,
  'picking a face leaves other profiles alone');
reset role;
select is(
  (select avatar_face::text from listing_cards where profile_id = '00000000-0000-0000-0000-00000000000a' limit 1), 'citra',
  'deck cards carry the picked face');
select pg_temp.login('00000000-0000-0000-0000-00000000000b');
select ok(
  (select bool_or(avatar_face = 'citra') from my_matches('30000000-0000-0000-0000-000000000001')),
  'matches carry the other side''s face');
reset role;


-- ---------------------------------------------------------------- demo students like back
insert into auth.users (id, email) values ('00000000-0000-0000-0000-0000000000d0', 'demo01@people.ether.app');
insert into listings (id, competition_id, kind, profile_id)
values ('40000000-0000-0000-0000-0000000000d0', '30000000-0000-0000-0000-000000000001', 'solo', '00000000-0000-0000-0000-0000000000d0');
insert into auth.users (id, email) values ('00000000-0000-0000-0000-0000000000d1', 'real.person@gmail.com');
insert into listings (id, competition_id, kind, profile_id)
values ('40000000-0000-0000-0000-0000000000d1', '30000000-0000-0000-0000-000000000001', 'solo', '00000000-0000-0000-0000-0000000000d1');
select is(
  (select direction::text from swipes where from_listing_id = '40000000-0000-0000-0000-0000000000d0'
     and to_listing_id = '40000000-0000-0000-0000-0000000000d1'), 'right',
  'a demo student likes a new real listing');
select is(
  (select count(*)::int from swipes where to_listing_id = '40000000-0000-0000-0000-0000000000d0'
     and from_listing_id in (select id from listings where profile_id <> '00000000-0000-0000-0000-0000000000d0')
     and swiped_by = '00000000-0000-0000-0000-0000000000d1'), 0,
  'real users never swipe automatically');
select pg_temp.login('00000000-0000-0000-0000-0000000000d1');
select is(
  (select (swipe('40000000-0000-0000-0000-0000000000d1', '40000000-0000-0000-0000-0000000000d0', 'right') ->> 'matched')::boolean), true,
  'a right swipe on a demo student matches');
select throws_ok($$ select is_demo_profile('00000000-0000-0000-0000-0000000000d0') $$, '42501', null,
  'clients cannot call the demo helpers');
reset role;

-- ---------------------------------------------------------------- guests (anon) browse competitions only
set local role anon;
select set_config('request.jwt.claims', json_build_object('role', 'anon')::text, true);
select ok((select count(*) from competitions) > 0, 'guests can read competitions');
select results_eq(
  $$ select organizer, mode::text, tags from competitions where id = '30000000-0000-0000-0000-0000000000d1' $$,
  $$ values ('Test Org'::text, 'online'::text, '{a,b,c,d,e,f,g,h}'::text[]) $$,
  'guests can read competition details');
select throws_ok(
  $$ update competitions set prize = 'free' where id = '30000000-0000-0000-0000-0000000000d1' $$,
  '42501', null, 'guests cannot edit competitions');
select throws_ok($$ select count(*) from profiles $$, '42501', null, 'guests cannot read profiles');
select throws_ok($$ select count(*) from listings $$, '42501', null, 'guests cannot read listings');
select throws_ok($$ select count(*) from profile_contacts $$, '42501', null, 'guests cannot read contacts');
select throws_ok($$ select count(*) from profile_hackathons $$, '42501', null, 'guests cannot read past hackathons');
select throws_ok($$ select count(*) from profile_projects $$, '42501', null, 'guests cannot read projects');
select throws_ok(
  $$ insert into profile_projects (profile_id, title, blurb) values ('00000000-0000-0000-0000-00000000000a', 'X', 'x') $$,
  '42501', null, 'guests cannot add projects');
reset role;

select * from finish();
rollback;
