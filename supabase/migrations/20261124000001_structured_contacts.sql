-- Structured contacts: a phone (opened in WhatsApp), an Instagram handle and a LINE ID replace the
-- free-text contact_url. Each is optional; a complete profile needs at least one. Telegram is dropped.
-- Visibility is unchanged: the read_matched policy still calls can_see_contact (owner, teammates,
-- or across a match).

-- Formats mirror the app's validation. Values are stored normalized: phone as digits with a country
-- code (the app writes no "+", but one is tolerated), handles without "@".
alter table public.profile_contacts
  add column phone     text check (phone ~ '^\+?[0-9]{8,16}$'),
  add column instagram text check (instagram ~ '^[A-Za-z0-9._]{1,30}$'),
  add column line_id   text check (line_id ~ '^[A-Za-z0-9._-]{4,20}$');

-- ---------------------------------------------------------------- backfill from contact_url
-- Parses every link shape the old form could hold: wa.me/<digits> (and api.whatsapp.com ?phone=),
-- instagram.com/<handle>, line.me/ti/p/~<id>. An Indonesian 0-prefixed number becomes 62….
-- Anything else (t.me links, a bare "https://wa.me") has no field to go to and is dropped.
update public.profile_contacts c set
  phone = (
    select case when d ~ '^0' then '62' || substr(d, 2) else d end
    from (select (regexp_match(c.contact_url, '(?:wa\.me/|[?&]phone=)\+?([0-9]+)', 'i'))[1] d) x
    where length(case when d ~ '^0' then '62' || substr(d, 2) else d end) between 8 and 16
  ),
  instagram = lower((regexp_match(c.contact_url, 'instagram\.com/@?([A-Za-z0-9._]{1,30})/?(?:[?#]|$)', 'i'))[1]),
  line_id = lower((regexp_match(c.contact_url, 'line\.me/ti/p/~([A-Za-z0-9._-]{4,20})/?(?:[?#]|$)', 'i'))[1]);

do $$
declare
  missed int;
  unparsed int;
begin
  -- Guard against a backfill bug: a wa.me link with a usable number must have produced a phone.
  select count(*) into missed from public.profile_contacts
  where phone is null and contact_url ~* '(wa\.me/|[?&]phone=)\+?[0-9]{8,16}([^0-9]|$)';
  if missed > 0 then
    raise exception 'structured_contacts backfill missed % parseable WhatsApp link(s)', missed;
  end if;
  select count(*) into unparsed from public.profile_contacts
  where phone is null and instagram is null and line_id is null;
  raise notice 'structured_contacts: % contact row(s) had no parseable WhatsApp, Instagram or LINE link', unparsed;
end $$;

-- my_matches returns the column, so it goes first; its return type changes below.
drop function public.my_matches(uuid);
alter table public.profile_contacts drop column contact_url;

-- ---------------------------------------------------------------- grants
-- Dropping contact_url removed its column grants. Table-level select and the RLS policies stay.
grant insert (profile_id, phone, instagram, line_id), update (phone, instagram, line_id)
  on public.profile_contacts to authenticated;

-- ---------------------------------------------------------------- profile completion

create or replace function public.check_profile_complete(p_profile uuid) returns void
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
      and coalesce(c.phone, c.instagram, c.line_id) is not null
  ) then
    perform award_xp(p_profile, 'profile_completed', p_profile, 50);
  end if;
end $$;

-- ---------------------------------------------------------------- my_matches

create function public.my_matches(p_competition uuid)
returns table (
  match_id uuid, matched_at timestamptz, my_listing_id uuid,
  listing_id uuid, kind listing_kind, profile_id uuid, team_id uuid, name text, headline text,
  skills text[], roles text[], open_roles text[], member_count int, xp int, reliability_score int,
  avatar_url text, phone text, instagram text, line_id text
)
language sql stable security definer set search_path = public as $$
  select m.id, m.created_at, side.mine,
         c.listing_id, c.kind, c.profile_id, c.team_id, c.name, c.headline,
         c.skills, c.roles, c.open_roles, c.member_count, c.xp, c.reliability_score,
         c.avatar_url, pc.phone, pc.instagram, pc.line_id
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
