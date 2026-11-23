-- Track record: past hackathons and projects on a profile, so teammates see what someone has built.
-- Readable by every signed-in user, written only by the owner, capped at 20 of each per profile.
-- The deck card (listing_cards) gains a compact summary: best result, hackathon count, project count.

-- Declared in rank order, so `order by result desc` puts the best result first.
create type public.hackathon_result as enum ('participant', 'honorable_mention', 'finalist', 'winner');

-- https only, with a dotted host and no whitespace. Shared by both tables.
create function public.is_https_url(p_url text) returns boolean
language sql immutable as $$
  select p_url ~ '^https://[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)+(:[0-9]{1,5})?([/?#][^[:space:]]*)?$'
     and char_length(p_url) <= 500
$$;

-- Each tag: 1–30 characters, no surrounding whitespace.
create function public.project_tags_valid(p_tags text[]) returns boolean
language sql immutable as $$
  select cardinality(p_tags) <= 6
     and coalesce((select bool_and(t is not null and t = btrim(t) and char_length(t) between 1 and 30)
                   from unnest(p_tags) t), true)
$$;

create table public.profile_hackathons (
  id           uuid primary key default gen_random_uuid(),
  profile_id   uuid not null references public.profiles (id) on delete cascade,
  name         text not null check (name = btrim(name) and char_length(name) between 1 and 80),
  -- The upper bound (current year + 1) moves with time, so the trigger below checks it.
  year         int not null check (year >= 2000),
  result       public.hackathon_result not null default 'participant',
  project_name text check (project_name = btrim(project_name) and char_length(project_name) between 1 and 80),
  url          text check (public.is_https_url(url)),
  created_at   timestamptz not null default now()
);
create index profile_hackathons_profile_idx on public.profile_hackathons (profile_id);

create table public.profile_projects (
  id         uuid primary key default gen_random_uuid(),
  profile_id uuid not null references public.profiles (id) on delete cascade,
  title      text not null check (title = btrim(title) and char_length(title) between 1 and 80),
  blurb      text not null check (blurb = btrim(blurb) and char_length(blurb) between 1 and 140),
  url        text check (public.is_https_url(url)),
  tags       text[] not null default '{}' check (public.project_tags_valid(tags)),
  created_at timestamptz not null default now()
);
create index profile_projects_profile_idx on public.profile_projects (profile_id);

-- ---------------------------------------------------------------- guards

-- 20 per profile per table. The per-profile advisory lock serializes concurrent inserts so two
-- requests cannot both see 19 rows and land at 21.
create function public.check_track_record_insert() returns trigger
language plpgsql security definer set search_path = public as $$
declare
  n int;
begin
  perform pg_advisory_xact_lock(hashtext(tg_table_name || ':' || new.profile_id::text));
  execute format('select count(*) from %I where profile_id = $1', tg_table_name) into n using new.profile_id;
  if n >= 20 then
    raise exception '%', case tg_table_name when 'profile_hackathons' then 'hackathon_limit_reached'
                                            else 'project_limit_reached' end;
  end if;
  return new;
end $$;

create trigger hackathon_limit before insert on public.profile_hackathons
  for each row execute function public.check_track_record_insert();
create trigger project_limit before insert on public.profile_projects
  for each row execute function public.check_track_record_insert();

-- Raised as a check violation (23514), like the static bounds in the table definition.
create function public.check_hackathon_year() returns trigger
language plpgsql as $$
begin
  if new.year > extract(year from now())::int + 1 then
    raise exception 'hackathon_year_out_of_range' using errcode = '23514';
  end if;
  return new;
end $$;

create trigger hackathon_year before insert or update of year on public.profile_hackathons
  for each row execute function public.check_hackathon_year();

-- ---------------------------------------------------------------- RLS and grants
-- Supabase's default privileges grant new tables to anon; revoke and rebuild like 20261015000003_rls.

alter table public.profile_hackathons enable row level security;
alter table public.profile_projects   enable row level security;
revoke all on public.profile_hackathons, public.profile_projects from anon, authenticated;
revoke execute on function
  public.is_https_url(text), public.project_tags_valid(text[]),
  public.check_track_record_insert(), public.check_hackathon_year()
from public, anon, authenticated;
-- Check constraints run as the writing role, so the pure validators must be callable by it.
grant execute on function public.is_https_url(text), public.project_tags_valid(text[]) to authenticated;

grant select, delete on public.profile_hackathons, public.profile_projects to authenticated;
grant insert (profile_id, name, year, result, project_name, url),
      update (name, year, result, project_name, url)
  on public.profile_hackathons to authenticated;
grant insert (profile_id, title, blurb, url, tags),
      update (title, blurb, url, tags)
  on public.profile_projects to authenticated;

create policy read_all on public.profile_hackathons for select to authenticated using (true);
create policy insert_own on public.profile_hackathons for insert to authenticated
  with check (profile_id = auth.uid());
create policy update_own on public.profile_hackathons for update to authenticated
  using (profile_id = auth.uid()) with check (profile_id = auth.uid());
create policy delete_own on public.profile_hackathons for delete to authenticated
  using (profile_id = auth.uid());

create policy read_all on public.profile_projects for select to authenticated using (true);
create policy insert_own on public.profile_projects for insert to authenticated
  with check (profile_id = auth.uid());
create policy update_own on public.profile_projects for update to authenticated
  using (profile_id = auth.uid()) with check (profile_id = auth.uid());
create policy delete_own on public.profile_projects for delete to authenticated
  using (profile_id = auth.uid());

-- ---------------------------------------------------------------- deck card summary
-- Same columns in the same order, plus five new ones at the end, so `create or replace` keeps the
-- view's grants (none for clients) and deck()'s `setof listing_cards` return type picks them up.
-- A solo card summarizes the profile; a team card summarizes all current members.

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
  coalesce(pc.n, 0)                           as project_count
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
