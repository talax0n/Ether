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

select throws_ok(
  $$ insert into auth.users (id, email) values ('00000000-0000-0000-0000-0000000000ff', 'mallory@gmail.com') $$,
  '42501', 'email_domain_not_allowed', 'sign-up with another domain is rejected');

select throws_ok(
  $$ insert into auth.users (id, email) values ('00000000-0000-0000-0000-0000000000fe', 'x@evil.student.example.ac.id.com') $$,
  '42501', 'email_domain_not_allowed', 'domain must be a suffix match');
select throws_ok(
  $$ insert into auth.users (id, email) values ('00000000-0000-0000-0000-0000000000fd', 'x@evil.student.example.ac.id') $$,
  '42501', 'email_domain_not_allowed', 'subdomain of the allowed domain is rejected');
select throws_ok(
  $$ insert into auth.users (id, email) values ('00000000-0000-0000-0000-0000000000fc', 'x@student.example.ac.id@evil.com') $$,
  '42501', 'email_domain_not_allowed', 'email with two @ is rejected');
select lives_ok(
  $$ insert into auth.users (id, email) values ('00000000-0000-0000-0000-0000000000fb', 'Upper@STUDENT.Example.AC.ID') $$,
  'domain match is case-insensitive');
select throws_ok(
  $$ update auth.users set email = 'upper@external.example' where id = '00000000-0000-0000-0000-0000000000fb' $$,
  '42501', 'email_domain_not_allowed', 'changing email to another domain is rejected');
select throws_ok(
  $$ update auth.users set email_change = 'upper@external.example' where id = '00000000-0000-0000-0000-0000000000fb' $$,
  '42501', 'email_domain_not_allowed', 'pending email change to another domain is rejected');

insert into auth.users (id, email)
select ('00000000-0000-0000-0000-0000000000' || x)::uuid, 'user' || x || '@student.example.ac.id'
from unnest(array['0a', '0b', '0c', '0d', '0e', '0f', '1a']) x;

select is((select name from profiles where id = '00000000-0000-0000-0000-00000000000a'), 'user0a',
  'allowed domain creates a profile named after the email');

insert into profile_contacts (profile_id, contact_url) values
  ('00000000-0000-0000-0000-00000000000a', 'https://wa.me/a'),
  ('00000000-0000-0000-0000-00000000000b', 'https://wa.me/b'),
  ('00000000-0000-0000-0000-00000000000c', 'https://wa.me/c');

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
  'contact_url hidden before match');

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
select is((select count(*)::int from matches), 0, 'no match after one-sided right');

select pg_temp.login('00000000-0000-0000-0000-00000000000b');
create temp table ab_result as
  select swipe('10000000-0000-0000-0000-00000000000b', '10000000-0000-0000-0000-00000000000a', 'right') r;
select is((select (r ->> 'matched')::boolean from ab_result), true, 'mutual right swipe matches');

select is(
  swipe('10000000-0000-0000-0000-00000000000b', '10000000-0000-0000-0000-00000000000a', 'right'),
  (select r from ab_result),
  'retrying the same swipe returns the same result');

reset role;
select is((select count(*)::int from matches), 1, 'mutual right creates exactly one match');
select is((select count(*)::int from swipes), 2, 'retry did not insert a second swipe');

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
select is((select contact_url from profile_contacts where profile_id = '00000000-0000-0000-0000-00000000000b'),
  'https://wa.me/b', 'contact_url visible after match');
select is((select contact_url from my_matches('30000000-0000-0000-0000-000000000001')),
  'https://wa.me/b', 'my_matches returns the other side''s contact');

reset role;
select pg_temp.login('00000000-0000-0000-0000-00000000000d');
select is(
  (select count(*)::int from profile_contacts where profile_id = '00000000-0000-0000-0000-00000000000a'), 0,
  'unmatched profile cannot read contact_url');
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
select is((select contact_url from my_matches('30000000-0000-0000-0000-000000000001')),
  'https://wa.me/c', 'solo sees the team owner''s contact');
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
select is((select contact_url from profile_contacts where profile_id = '00000000-0000-0000-0000-00000000000c'),
  'https://wa.me/c', 'matched solo reads team owner contact via RLS');
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
insert into profile_contacts (profile_id, contact_url) values ('00000000-0000-0000-0000-00000000000d', 'https://t.me/d');
update profiles set major = 'CS', year = 2, bio = 'hi', skills = '{Go}', roles = '{Backend}'
where id = '00000000-0000-0000-0000-00000000000d';
reset role;
select is((select xp from profiles where id = '00000000-0000-0000-0000-00000000000d'), 50, 'profile completed awards 50 XP');

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
select is((select contact_url from profile_contacts where profile_id = '00000000-0000-0000-0000-00000000000c'),
  'https://wa.me/c', 'teammates can read each other''s contact without a match');
reset role;

select * from finish();
rollback;
