-- Competition details: who runs it, what it pays, where it happens, and topic tags, so the Compete tab
-- can search and filter. Competitions stay hand-entered by the team (no scraping); `anon` keeps its
-- read access from 20261123000001_guest_browse, which covers new columns automatically.

create type public.competition_mode as enum ('online', 'offline', 'hybrid');

-- Each tag: 1–30 characters, no surrounding whitespace, at most 8, no case-insensitive duplicates.
create function public.competition_tags_valid(p_tags text[]) returns boolean
language sql immutable as $$
  select cardinality(p_tags) <= 8
     and coalesce((select bool_and(t is not null and t = btrim(t) and char_length(t) between 1 and 30)
                   from unnest(p_tags) t), true)
     and (select count(distinct lower(t)) from unnest(p_tags) t) = cardinality(p_tags)
$$;

alter table public.competitions
  add column organizer text
    constraint competitions_organizer_check check (organizer = btrim(organizer) and char_length(organizer) between 1 and 80),
  add column prize text
    constraint competitions_prize_check check (prize = btrim(prize) and char_length(prize) between 1 and 80),
  add column mode public.competition_mode not null default 'online',
  add column city text
    constraint competitions_city_check check (city = btrim(city) and char_length(city) between 1 and 60),
  add column tags text[] not null default '{}'
    constraint competitions_tags_check check (public.competition_tags_valid(tags)),
  -- An online event has no venue, so a city would only mislead the filters.
  add constraint competitions_city_mode_check check (mode <> 'online' or city is null),
  -- The official page. is_https_url comes from 20261124000002_track_record.
  add constraint competitions_url_check check (public.is_https_url(url));
