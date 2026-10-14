-- Atomic team creation, and contact links visible between teammates.

-- One transaction for team, owner membership and TEAM listing, so a failure never leaves a team
-- without its listing. The teams/listings triggers still run and stay the source of truth; the
-- checks here only fail fast with the same messages before anything is written.
create function public.create_team(p_competition uuid, p_name text, p_pitch text, p_open_roles text[])
returns uuid
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  deadline timestamptz;
  new_team uuid;
begin
  if me is null then
    raise exception 'not_authenticated' using errcode = '42501';
  end if;
  select registration_deadline into deadline from competitions where id = p_competition;
  if deadline is null then
    raise exception 'competition_not_found';
  end if;
  if now() > deadline then
    raise exception 'registration_closed';
  end if;
  if nullif(trim(p_name), '') is null then
    raise exception 'team_name_required';
  end if;
  if exists (select 1 from team_members where profile_id = me and competition_id = p_competition) then
    raise exception 'already_in_team';
  end if;

  insert into teams (competition_id, owner_id, name, pitch, open_roles)
  values (p_competition, me, trim(p_name), nullif(trim(p_pitch), ''), coalesce(p_open_roles, '{}'))
  returning id into new_team;
  insert into listings (competition_id, kind, team_id) values (p_competition, 'team', new_team);
  return new_team;
end $$;

-- Teammates need each other's contact after the event (and to coordinate before it), even when
-- they never matched directly, e.g. two solos who each matched the same team.
create or replace function public.can_see_contact(p_target uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select p_target = auth.uid()
    or exists (
      select 1 from team_members a join team_members b on b.team_id = a.team_id
      where a.profile_id = auth.uid() and b.profile_id = p_target
    )
    or exists (
      select 1 from matches m,
        lateral (values (m.listing_a, m.listing_b), (m.listing_b, m.listing_a)) x(mine, other)
      where is_listing_member(x.mine, auth.uid()) and is_listing_member(x.other, p_target)
    )
$$;

revoke execute on function public.create_team(uuid, text, text, text[]) from public, anon;
grant execute on function public.create_team(uuid, text, text, text[]) to authenticated;
