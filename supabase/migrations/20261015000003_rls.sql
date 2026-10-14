-- Row level security and grants. Default Supabase grants are revoked and rebuilt explicitly:
-- clients never write xp, matches, swipes, team_members, badges or the XP ledger directly.

revoke all on all tables in schema public from anon, authenticated;
revoke execute on all functions in schema public from public, anon, authenticated;

alter table public.app_config       enable row level security;
alter table public.profiles         enable row level security;
alter table public.profile_contacts enable row level security;
alter table public.competitions     enable row level security;
alter table public.teams            enable row level security;
alter table public.team_members     enable row level security;
alter table public.listings         enable row level security;
alter table public.swipes           enable row level security;
alter table public.matches          enable row level security;
alter table public.endorsements     enable row level security;
alter table public.badges           enable row level security;
alter table public.profile_badges   enable row level security;
alter table public.xp_events        enable row level security;
alter table public.team_results     enable row level security;

grant select on public.app_config, public.profiles, public.competitions, public.teams, public.team_members,
  public.listings, public.badges, public.profile_badges, public.team_results to authenticated;
create policy read_all on public.app_config     for select to authenticated using (true);
create policy read_all on public.profiles       for select to authenticated using (true);
create policy read_all on public.competitions   for select to authenticated using (true);
create policy read_all on public.teams          for select to authenticated using (true);
create policy read_all on public.team_members   for select to authenticated using (true);
create policy read_all on public.listings       for select to authenticated using (true);
create policy read_all on public.badges         for select to authenticated using (true);
create policy read_all on public.profile_badges for select to authenticated using (true);
create policy read_all on public.team_results   for select to authenticated using (true);

-- profiles: rows are created by the sign-up trigger; xp is not in the column grant.
grant update (name, major, year, bio, skills, roles, avatar_url) on public.profiles to authenticated;
create policy update_own on public.profiles for update to authenticated
  using (id = auth.uid()) with check (id = auth.uid());

grant select, insert (profile_id, contact_url), update (contact_url) on public.profile_contacts to authenticated;
create policy read_matched on public.profile_contacts for select to authenticated
  using (public.can_see_contact(profile_id));
create policy insert_own on public.profile_contacts for insert to authenticated
  with check (profile_id = auth.uid());
create policy update_own on public.profile_contacts for update to authenticated
  using (profile_id = auth.uid()) with check (profile_id = auth.uid());

grant insert (competition_id, owner_id, name, pitch, open_roles) on public.teams to authenticated;
grant update (name, pitch, open_roles) on public.teams to authenticated;
create policy insert_own on public.teams for insert to authenticated
  with check (owner_id = auth.uid());
create policy update_own on public.teams for update to authenticated
  using (owner_id = auth.uid()) with check (owner_id = auth.uid());

grant insert (competition_id, kind, profile_id, team_id, active) on public.listings to authenticated;
grant update (active) on public.listings to authenticated;
create policy insert_own on public.listings for insert to authenticated
  with check (
    (kind = 'solo' and profile_id = auth.uid()) or
    (kind = 'team' and exists (select 1 from public.teams t where t.id = team_id and t.owner_id = auth.uid()))
  );
create policy update_own on public.listings for update to authenticated
  using (public.listing_actor(id) = auth.uid()) with check (public.listing_actor(id) = auth.uid());

grant select on public.swipes to authenticated;
create policy read_own on public.swipes for select to authenticated using (swiped_by = auth.uid());

grant select on public.matches to authenticated;
create policy read_own on public.matches for select to authenticated
  using (public.is_listing_member(listing_a, auth.uid()) or public.is_listing_member(listing_b, auth.uid()));

grant select, insert (from_profile_id, to_profile_id, competition_id, positive) on public.endorsements to authenticated;
create policy read_own on public.endorsements for select to authenticated
  using (from_profile_id = auth.uid() or to_profile_id = auth.uid());
create policy insert_own on public.endorsements for insert to authenticated
  with check (from_profile_id = auth.uid());

grant select on public.xp_events to authenticated;
create policy read_own on public.xp_events for select to authenticated using (profile_id = auth.uid());

-- RPCs, plus the helpers that policies above call as the querying role.
grant execute on function
  public.swipe(uuid, uuid, public.swipe_direction),
  public.deck(uuid, int),
  public.remaining_swipes(),
  public.my_matches(uuid),
  public.reliability_score(uuid),
  public.report_result(uuid, public.team_result),
  public.confirm_result(uuid),
  public.can_see_contact(uuid),
  public.is_listing_member(uuid, uuid),
  public.listing_actor(uuid)
to authenticated;
