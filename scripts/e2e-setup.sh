#!/usr/bin/env bash
# Seeds the state the Maestro flows expect on top of `supabase db reset`. Safe to run repeatedly.
set -euo pipefail
DB_URL="${DB_URL:-postgresql://postgres:postgres@127.0.0.1:54322/postgres}"

psql "$DB_URL" -v ON_ERROR_STOP=1 -q -o /dev/null <<'SQL'
-- swipe_and_match: Byte Club (owner demo7) has already swiped right on demo3's solo listing.
-- swipe() checks the caller through auth.uid(), so run it as demo7. Retrying returns the existing swipe.
begin;
select set_config('request.jwt.claims', '{"sub":"d0000000-0000-0000-0000-000000000007","role":"authenticated"}', true);
set local role authenticated;
select public.swipe(
  (select id from public.listings where team_id = 'a0000000-0000-0000-0000-000000000001'),
  'b0000000-0000-0000-0000-000000000003',
  'right');
commit;

-- create_team: demo5 enters Campus Hackathon without a listing, so the create-team option shows.
delete from public.listings l
where l.profile_id = 'd0000000-0000-0000-0000-000000000005'
  and l.competition_id = 'c0000000-0000-0000-0000-000000000001'
  and not exists (select 1 from public.swipes s where s.from_listing_id = l.id or s.to_listing_id = l.id);
SQL
echo "e2e setup done"
