-- Demo students (accounts at people.ether.app, seeded by supabase/snippets/demo_people.sql) like
-- every real listing in their competition, so a right swipe on one of them forms a match.
-- Demo only: delete the demo accounts and this has nothing to act on.

create function public.is_demo_profile(p_profile uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from auth.users u where u.id = p_profile and u.email like '%@people.ether.app')
$$;

revoke execute on function public.is_demo_profile(uuid) from public, anon, authenticated;

create function public.demo_like_back(p_listing uuid) returns void
language sql security definer set search_path = public as $$
  insert into swipes (from_listing_id, to_listing_id, direction, swiped_by)
  select d.id, l.id, 'right', listing_actor(d.id)
  from listings l
  join listings d on d.competition_id = l.competition_id and d.id <> l.id and d.active
  where l.id = p_listing
    and not is_demo_profile(listing_actor(l.id))
    and is_demo_profile(listing_actor(d.id))
    and not (d.kind = 'team' and l.kind = 'team')
  on conflict (from_listing_id, to_listing_id) do nothing
$$;

revoke execute on function public.demo_like_back(uuid) from public, anon, authenticated;

create function public.on_listing_created_demo() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  perform demo_like_back(new.id);
  return null;
end $$;

create trigger listing_demo_like_back after insert on public.listings
  for each row execute function public.on_listing_created_demo();

-- Listings that already exist.
select demo_like_back(id) from listings;
