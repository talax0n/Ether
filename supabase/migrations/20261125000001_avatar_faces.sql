-- Illustrated faces a user can pick as their picture. The picked face shows instead of the photo
-- (Google's, if any) and the initials. The app bundles one drawing per value.

create type public.avatar_face as enum ('nadia', 'budi', 'citra', 'dimas', 'eka', 'fajar');

alter table public.profiles add column avatar_face avatar_face;

grant update (avatar_face) on public.profiles to authenticated;

-- ---------------------------------------------------------------- deck card summary
-- Same columns in the same order plus avatar_face at the end, so `create or replace` keeps grants
-- and deck()'s `setof listing_cards` return type picks it up. A team shows its owner's face.

create or replace view public.listing_cards as
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
  l.created_at,
  best.result                                 as best_result,
  best.name                                   as best_hackathon,
  best.year                                   as best_hackathon_year,
  coalesce(hc.n, 0)                           as hackathon_count,
  coalesce(pc.n, 0)                           as project_count,
  coalesce(p.avatar_face, o.avatar_face)      as avatar_face
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
) agg on true
left join lateral (
  select case when l.kind = 'solo' then array[l.profile_id]
              else array(select tm.profile_id from team_members tm where tm.team_id = l.team_id) end as ids
) people on true
left join lateral (
  -- Best result first, then the most recent.
  select h.result, h.name, h.year from profile_hackathons h
  where h.profile_id = any (people.ids)
  order by h.result desc, h.year desc, h.created_at desc
  limit 1
) best on true
left join lateral (
  select count(*)::int as n from profile_hackathons h where h.profile_id = any (people.ids)
) hc on true
left join lateral (
  select count(*)::int as n from profile_projects pp where pp.profile_id = any (people.ids)
) pc on true;

-- ---------------------------------------------------------------- my_matches
-- The return type gains avatar_face, so drop and recreate.

drop function public.my_matches(uuid);

create function public.my_matches(p_competition uuid)
returns table (
  match_id uuid, matched_at timestamptz, my_listing_id uuid,
  listing_id uuid, kind listing_kind, profile_id uuid, team_id uuid, name text, headline text,
  skills text[], roles text[], open_roles text[], member_count int, xp int, reliability_score int,
  avatar_url text, phone text, instagram text, line_id text, avatar_face avatar_face
)
language sql stable security definer set search_path = public as $$
  select m.id, m.created_at, side.mine,
         c.listing_id, c.kind, c.profile_id, c.team_id, c.name, c.headline,
         c.skills, c.roles, c.open_roles, c.member_count, c.xp, c.reliability_score,
         c.avatar_url, pc.phone, pc.instagram, pc.line_id, c.avatar_face
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

revoke execute on function public.my_matches(uuid) from public, anon;
grant execute on function public.my_matches(uuid) to authenticated;
