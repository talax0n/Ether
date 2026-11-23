-- Guests browse before signing up: competitions are public listings, readable without an account.
-- Everything else (profiles, listings, teams, matches, contacts) stays signed-in only.
grant select on public.competitions to anon;
create policy read_all_anon on public.competitions for select to anon using (true);
