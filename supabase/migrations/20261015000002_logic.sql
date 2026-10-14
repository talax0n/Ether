-- Business rules: sign-up, team formation, matching, deck, XP and badges (SPEC §3, §4).
-- Errors are raised with stable snake_case messages listed in supabase/CONTRACT.md.

-- ---------------------------------------------------------------- helpers

create function public.listing_actor(p_listing uuid) returns uuid
language sql stable security definer set search_path = public as $$
  select coalesce(l.profile_id, t.owner_id)
  from listings l left join teams t on t.id = l.team_id
  where l.id = p_listing
$$;

create function public.is_listing_member(p_listing uuid, p_profile uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from listings l
    where l.id = p_listing
      and (l.profile_id = p_profile
           or exists (select 1 from team_members tm where tm.team_id = l.team_id and tm.profile_id = p_profile))
  )
$$;

create function public.reliability_score(p_profile uuid) returns int
language sql stable security definer set search_path = public as $$
  select count(*)::int from endorsements where to_profile_id = p_profile and positive
$$;

create function public.swipes_today(p_profile uuid) returns int
language sql stable security definer set search_path = public as $$
  select count(*)::int from swipes
  where swiped_by = p_profile
    and created_at >= date_trunc('day', now() at time zone 'utc') at time zone 'utc'
$$;

create function public.award_xp(p_profile uuid, p_reason xp_reason, p_ref uuid, p_amount int) returns boolean
language plpgsql security definer set search_path = public as $$
declare
  inserted int;
begin
  insert into xp_events (profile_id, reason, ref_id, amount)
  values (p_profile, p_reason, p_ref, p_amount)
  on conflict do nothing;
  get diagnostics inserted = row_count;
  if inserted > 0 then
    update profiles set xp = xp + p_amount where id = p_profile;
  end if;
  return inserted > 0;
end $$;

create function public.award_badge(p_profile uuid, p_code text) returns void
language sql security definer set search_path = public as $$
  insert into profile_badges (profile_id, badge_code) values (p_profile, p_code) on conflict do nothing
$$;

-- ---------------------------------------------------------------- sign-up

create function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
declare
  domain text := (select value from app_config where key = 'allowed_email_domain');
begin
  if new.email is null or lower(new.email) not like '%@' || lower(domain) then
    raise exception 'email_domain_not_allowed' using errcode = '42501';
  end if;
  insert into profiles (id, name)
  values (new.id, coalesce(nullif(new.raw_user_meta_data ->> 'name', ''), split_part(new.email, '@', 1)));
  return new;
end $$;

create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------- profile completion XP

create function public.check_profile_complete(p_profile uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
  if exists (
    select 1 from profiles p join profile_contacts c on c.profile_id = p.id
    where p.id = p_profile
      and nullif(trim(p.name), '') is not null
      and nullif(trim(p.major), '') is not null
      and p.year is not null
      and nullif(trim(p.bio), '') is not null
      and cardinality(p.skills) > 0
      and cardinality(p.roles) > 0
      and nullif(trim(c.contact_url), '') is not null
  ) then
    perform award_xp(p_profile, 'profile_completed', p_profile, 50);
  end if;
end $$;

create function public.on_profile_changed() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  perform check_profile_complete((to_jsonb(new) ->> case tg_table_name when 'profiles' then 'id' else 'profile_id' end)::uuid);
  return null;
end $$;

create trigger profile_completed after update of name, major, year, bio, skills, roles on public.profiles
  for each row execute function public.on_profile_changed();
create trigger contact_completed after insert or update on public.profile_contacts
  for each row execute function public.on_profile_changed();

-- ---------------------------------------------------------------- teams and listings

create function public.on_team_created() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into team_members (team_id, profile_id) values (new.id, new.owner_id);
  return null;
end $$;

create trigger team_owner_is_member after insert on public.teams
  for each row execute function public.on_team_created();

create function public.on_team_member_added() returns trigger
language plpgsql security definer set search_path = public as $$
declare
  comp competitions;
  member_count int;
  m uuid;
begin
  -- Row lock serializes concurrent joins so the size check below cannot be raced.
  select c.* into comp from teams t join competitions c on c.id = t.competition_id
  where t.id = new.team_id for update of t;

  if exists (
    select 1 from team_members tm join teams t on t.id = tm.team_id
    where tm.profile_id = new.profile_id and t.competition_id = comp.id and tm.team_id <> new.team_id
  ) then
    raise exception 'already_in_team';
  end if;

  select count(*) into member_count from team_members where team_id = new.team_id;
  if member_count > comp.max_team_size then
    raise exception 'team_full';
  end if;

  update listings set active = false
  where kind = 'solo' and profile_id = new.profile_id and competition_id = comp.id;
  if member_count >= comp.max_team_size then
    update listings set active = false where team_id = new.team_id;
  end if;

  -- A team counts as formed once it has two people; the owner earns it when the first teammate joins.
  if member_count >= 2 then
    for m in select profile_id from team_members where team_id = new.team_id loop
      if award_xp(m, 'team_joined', new.team_id, 100) then
        perform award_badge(m, 'first_team');
      end if;
    end loop;
  end if;
  return null;
end $$;

create trigger team_member_added after insert on public.team_members
  for each row execute function public.on_team_member_added();

create function public.check_listing_active() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if not new.active then
    return new;
  end if;
  if new.kind = 'solo' and exists (
    select 1 from team_members tm join teams t on t.id = tm.team_id
    where tm.profile_id = new.profile_id and t.competition_id = new.competition_id
  ) then
    raise exception 'already_in_team';
  end if;
  if new.kind = 'team' and (select count(*) from team_members where team_id = new.team_id)
      >= (select max_team_size from competitions where id = new.competition_id) then
    raise exception 'team_full';
  end if;
  return new;
end $$;

create trigger listing_active_guard before insert or update of active on public.listings
  for each row execute function public.check_listing_active();

-- ---------------------------------------------------------------- matching

-- SOLO↔TEAM: the solo joins the team. SOLO↔SOLO: the profile that swiped right first owns a
-- new team with both members and a TEAM listing. Listing deactivation happens in on_team_member_added.
create function public.on_match_created() returns trigger
language plpgsql security definer set search_path = public as $$
declare
  la listings;
  lb listings;
  v_owner uuid;
  other_id uuid;
  new_team uuid;
begin
  select * into la from listings where id = new.listing_a;
  select * into lb from listings where id = new.listing_b;

  perform award_xp(listing_actor(la.id), 'first_match', listing_actor(la.id), 25);
  perform award_xp(listing_actor(lb.id), 'first_match', listing_actor(lb.id), 25);

  if la.kind = 'solo' and lb.kind = 'solo' then
    select s.swiped_by into v_owner from swipes s
    where s.direction = 'right'
      and ((s.from_listing_id, s.to_listing_id) in ((la.id, lb.id), (lb.id, la.id)))
    order by s.created_at
    limit 1;
    other_id := case when v_owner = la.profile_id then lb.profile_id else la.profile_id end;

    insert into teams (competition_id, owner_id, name)
    values (new.competition_id, v_owner,
            (select name from profiles where id = v_owner) || ' & ' || (select name from profiles where id = other_id))
    returning id into new_team;
    insert into team_members (team_id, profile_id) values (new_team, other_id);
    insert into listings (competition_id, kind, team_id, active)
    values (new.competition_id, 'team', new_team,
            (select count(*) from team_members where team_id = new_team)
              < (select max_team_size from competitions where id = new.competition_id));
  elsif la.kind = 'team' then
    insert into team_members (team_id, profile_id) values (la.team_id, lb.profile_id);
  else
    insert into team_members (team_id, profile_id) values (lb.team_id, la.profile_id);
  end if;
  return null;
end $$;

create trigger match_created after insert on public.matches
  for each row execute function public.on_match_created();

create function public.swipe(p_from uuid, p_to uuid, p_direction swipe_direction) returns jsonb
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  f listings;
  t listings;
  match_id uuid;
begin
  if me is null then
    raise exception 'not_authenticated' using errcode = '42501';
  end if;
  select * into f from listings where id = p_from;
  select * into t from listings where id = p_to;
  if f.id is null or t.id is null then
    raise exception 'listing_not_found';
  end if;
  if listing_actor(f.id) is distinct from me then
    raise exception 'not_listing_owner' using errcode = '42501';
  end if;

  -- Serialize the pair so two opposite right swipes cannot both miss each other,
  -- and the caller so the daily cap cannot be raced.
  perform pg_advisory_xact_lock(hashtext(least(p_from, p_to)::text || greatest(p_from, p_to)::text));
  perform pg_advisory_xact_lock(hashtext(me::text));

  -- Retry of an existing swipe returns the original outcome, even if listings went inactive since.
  if not exists (select 1 from swipes where from_listing_id = p_from and to_listing_id = p_to) then
    if p_from = p_to or listing_actor(t.id) = me then
      raise exception 'self_swipe';
    end if;
    if f.competition_id <> t.competition_id then
      raise exception 'different_competition';
    end if;
    if not f.active or not t.active then
      raise exception 'listing_inactive';
    end if;
    if f.kind = 'team' and t.kind = 'team' then
      raise exception 'team_to_team';
    end if;
    if swipes_today(me) >= 30 then
      raise exception 'swipe_cap_reached';
    end if;

    insert into swipes (from_listing_id, to_listing_id, direction, swiped_by)
    values (p_from, p_to, p_direction, me);

    if p_direction = 'right' and exists (
      select 1 from swipes where from_listing_id = p_to and to_listing_id = p_from and direction = 'right'
    ) then
      insert into matches (competition_id, listing_a, listing_b)
      values (f.competition_id, least(p_from, p_to), greatest(p_from, p_to))
      on conflict do nothing;
    end if;
  end if;

  select m.id into match_id from matches m
  where m.listing_a = least(p_from, p_to) and m.listing_b = greatest(p_from, p_to);

  return jsonb_build_object(
    'matched', match_id is not null,
    'match_id', match_id,
    'remaining', greatest(30 - swipes_today(me), 0)
  );
end $$;

create function public.remaining_swipes() returns int
language sql stable security definer set search_path = public as $$
  select greatest(30 - swipes_today(auth.uid()), 0)
$$;

-- ---------------------------------------------------------------- cards, deck, matches

-- Not granted to clients: it bypasses RLS and is only read through deck() and my_matches().
create view public.listing_cards as
select
  l.id                                        as listing_id,
  l.competition_id,
  l.kind,
  l.profile_id,
  l.team_id,
  coalesce(l.profile_id, t.owner_id)          as owner_id,
  coalesce(p.name, t.name)                    as name,
  coalesce(t.pitch, p.bio)                    as headline,
  case when l.kind = 'solo' then p.skills else
    array(select distinct x from team_members tm join profiles mp on mp.id = tm.profile_id,
          unnest(mp.skills) x where tm.team_id = l.team_id order by x) end as skills,
  case when l.kind = 'solo' then p.roles else
    array(select distinct x from team_members tm join profiles mp on mp.id = tm.profile_id,
          unnest(mp.roles) x where tm.team_id = l.team_id order by x) end as roles,
  coalesce(t.open_roles, '{}')                as open_roles,
  case when l.kind = 'solo' then 1 else agg.member_count end            as member_count,
  case when l.kind = 'solo' then p.xp else agg.xp end                   as xp,
  case when l.kind = 'solo' then reliability_score(p.id) else agg.reliability end as reliability_score,
  coalesce(p.avatar_url, o.avatar_url)        as avatar_url,
  l.active,
  l.created_at
from listings l
left join profiles p on p.id = l.profile_id
left join teams t on t.id = l.team_id
left join profiles o on o.id = t.owner_id
left join lateral (
  select count(*)::int as member_count,
         coalesce(sum(mp.xp), 0)::int as xp,
         coalesce(sum(reliability_score(mp.id)), 0)::int as reliability
  from team_members tm join profiles mp on mp.id = tm.profile_id
  where tm.team_id = l.team_id
) agg on true;

create function public.deck(p_listing uuid, p_limit int default 20) returns setof public.listing_cards
language plpgsql stable security definer set search_path = public as $$
declare
  me listings;
begin
  select * into me from listings where id = p_listing;
  if me.id is null or listing_actor(me.id) is distinct from auth.uid() then
    raise exception 'not_listing_owner' using errcode = '42501';
  end if;
  return query
    select c.* from listing_cards c
    where c.competition_id = me.competition_id
      and c.active
      and c.listing_id <> me.id
      and c.owner_id <> auth.uid()
      and not (me.kind = 'team' and c.kind = 'team')
      and not exists (select 1 from swipes s where s.from_listing_id = me.id and s.to_listing_id = c.listing_id)
    order by c.created_at desc, c.listing_id
    limit least(greatest(p_limit, 0), 100);
end $$;

create function public.my_matches(p_competition uuid)
returns table (
  match_id uuid, matched_at timestamptz, my_listing_id uuid,
  listing_id uuid, kind listing_kind, profile_id uuid, team_id uuid, name text, headline text,
  skills text[], roles text[], open_roles text[], member_count int, xp int, reliability_score int,
  avatar_url text, contact_url text
)
language sql stable security definer set search_path = public as $$
  select m.id, m.created_at, side.mine,
         c.listing_id, c.kind, c.profile_id, c.team_id, c.name, c.headline,
         c.skills, c.roles, c.open_roles, c.member_count, c.xp, c.reliability_score,
         c.avatar_url, pc.contact_url
  from matches m
  cross join lateral (
    -- After SOLO↔TEAM the solo is in both listings; prefer the side the caller swiped from.
    select x.mine, x.other
    from (values (m.listing_a, m.listing_b), (m.listing_b, m.listing_a)) x(mine, other)
    where is_listing_member(x.mine, auth.uid())
    order by listing_actor(x.mine) = auth.uid() desc
    limit 1
  ) side
  join listing_cards c on c.listing_id = side.other
  left join profile_contacts pc on pc.profile_id = c.owner_id
  where m.competition_id = p_competition
  order by m.created_at desc
$$;

-- RLS helper for profile_contacts: owner, or across a match (team listings include all members).
-- ponytail: scans the caller's matches per row; fine at campus scale, index matches by member if it grows.
create function public.can_see_contact(p_target uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select p_target = auth.uid() or exists (
    select 1 from matches m,
      lateral (values (m.listing_a, m.listing_b), (m.listing_b, m.listing_a)) x(mine, other)
    where is_listing_member(x.mine, auth.uid()) and is_listing_member(x.other, p_target)
  )
$$;

-- ---------------------------------------------------------------- endorsements and results

create function public.check_endorsement() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if not exists (
    select 1 from team_members a
    join team_members b on b.team_id = a.team_id
    join teams t on t.id = a.team_id
    where a.profile_id = new.from_profile_id and b.profile_id = new.to_profile_id
      and t.competition_id = new.competition_id
  ) then
    raise exception 'not_teammates';
  end if;
  if (select event_end from competitions where id = new.competition_id) > now() then
    raise exception 'event_not_ended';
  end if;
  return new;
end $$;

create trigger endorsement_guard before insert on public.endorsements
  for each row execute function public.check_endorsement();

create function public.on_endorsement_created() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if new.positive and award_xp(new.to_profile_id, 'endorsement_received', new.id, 30)
     and reliability_score(new.to_profile_id) >= 5 then
    perform award_badge(new.to_profile_id, 'reliable');
  end if;
  return null;
end $$;

create trigger endorsement_created after insert on public.endorsements
  for each row execute function public.on_endorsement_created();

-- Admin action (SQL editor / service role): awards "competition finished" XP to every team member.
create function public.finish_competition(p_competition uuid) returns void
language plpgsql security definer set search_path = public as $$
declare
  m uuid;
begin
  if (select event_end from competitions where id = p_competition) > now() then
    raise exception 'event_not_ended';
  end if;
  for m in
    select distinct tm.profile_id from team_members tm join teams t on t.id = tm.team_id
    where t.competition_id = p_competition
  loop
    perform award_xp(m, 'competition_finished', p_competition, 150);
    if (select count(*) from xp_events where profile_id = m and reason = 'competition_finished') >= 3 then
      perform award_badge(m, 'three_hackathons');
    end if;
  end loop;
end $$;

create function public.assert_team_member_after_event(p_team uuid) returns void
language plpgsql stable security definer set search_path = public as $$
begin
  if not exists (select 1 from team_members where team_id = p_team and profile_id = auth.uid()) then
    raise exception 'not_team_member' using errcode = '42501';
  end if;
  if (select c.event_end from teams t join competitions c on c.id = t.competition_id where t.id = p_team) > now() then
    raise exception 'event_not_ended';
  end if;
end $$;

create function public.report_result(p_team uuid, p_result team_result) returns void
language plpgsql security definer set search_path = public as $$
begin
  perform assert_team_member_after_event(p_team);
  if exists (select 1 from team_results where team_id = p_team and confirmed_by is not null) then
    raise exception 'result_already_confirmed';
  end if;
  insert into team_results (team_id, result, reported_by) values (p_team, p_result, auth.uid())
  on conflict (team_id) do update set result = excluded.result, reported_by = excluded.reported_by;
end $$;

create function public.confirm_result(p_team uuid) returns void
language plpgsql security definer set search_path = public as $$
declare
  r team_results;
  m uuid;
begin
  perform assert_team_member_after_event(p_team);
  select * into r from team_results where team_id = p_team for update;
  if r.team_id is null then
    raise exception 'no_result_reported';
  end if;
  if r.reported_by = auth.uid() then
    raise exception 'cannot_confirm_own_result';
  end if;
  if r.confirmed_by is not null then
    return;
  end if;
  update team_results set confirmed_by = auth.uid() where team_id = p_team;
  for m in select profile_id from team_members where team_id = p_team loop
    perform award_xp(m, 'result_confirmed', p_team, 200);
    perform award_badge(m, r.result::text);
  end loop;
end $$;
