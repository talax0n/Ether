-- Ether schema: SPEC §3 domain model plus the gamification ledger from SPEC §4.

create type public.listing_kind as enum ('solo', 'team');
create type public.swipe_direction as enum ('left', 'right');
create type public.xp_reason as enum (
  'profile_completed', 'first_match', 'team_joined',
  'competition_finished', 'endorsement_received', 'result_confirmed'
);
create type public.team_result as enum ('finalist', 'winner');

create table public.app_config (
  key   text primary key,
  value text not null
);
insert into public.app_config (key, value) values ('allowed_email_domain', 'student.example.ac.id');

create table public.profiles (
  id         uuid primary key references auth.users (id) on delete cascade,
  name       text not null,
  major      text,
  year       int check (year between 1 and 10),
  bio        text,
  skills     text[] not null default '{}',
  roles      text[] not null default '{}',
  avatar_url text,
  xp         int not null default 0,
  created_at timestamptz not null default now()
);

create table public.profile_contacts (
  profile_id  uuid primary key references public.profiles (id) on delete cascade,
  contact_url text not null
);

create table public.competitions (
  id                    uuid primary key default gen_random_uuid(),
  name                  text not null,
  description           text,
  url                   text,
  registration_deadline timestamptz not null,
  event_end             timestamptz not null,
  min_team_size         int not null default 1 check (min_team_size >= 1),
  max_team_size         int not null check (max_team_size >= 2),
  check (min_team_size <= max_team_size),
  check (registration_deadline <= event_end)
);

create table public.teams (
  id             uuid primary key default gen_random_uuid(),
  competition_id uuid not null references public.competitions (id) on delete cascade,
  owner_id       uuid not null references public.profiles (id) on delete cascade,
  name           text not null,
  pitch          text,
  open_roles     text[] not null default '{}',
  created_at     timestamptz not null default now(),
  -- target of the composite FKs that keep listings/team_members.competition_id consistent with the team
  unique (id, competition_id)
);

create table public.team_members (
  team_id        uuid not null,
  -- filled from the team by a trigger; denormalized so one-team-per-competition is a unique constraint
  competition_id uuid not null,
  profile_id     uuid not null references public.profiles (id) on delete cascade,
  joined_at      timestamptz not null default now(),
  primary key (team_id, profile_id),
  foreign key (team_id, competition_id) references public.teams (id, competition_id) on delete cascade,
  unique (profile_id, competition_id)
);

create table public.listings (
  id             uuid primary key default gen_random_uuid(),
  competition_id uuid not null references public.competitions (id) on delete cascade,
  kind           public.listing_kind not null,
  profile_id     uuid references public.profiles (id) on delete cascade,
  team_id        uuid,
  active         boolean not null default true,
  created_at     timestamptz not null default now(),
  foreign key (team_id, competition_id) references public.teams (id, competition_id) on delete cascade,
  check (
    (kind = 'solo' and profile_id is not null and team_id is null) or
    (kind = 'team' and team_id is not null and profile_id is null)
  ),
  unique (competition_id, profile_id),
  unique (competition_id, team_id)
);

create table public.swipes (
  id              uuid primary key default gen_random_uuid(),
  from_listing_id uuid not null references public.listings (id) on delete cascade,
  to_listing_id   uuid not null references public.listings (id) on delete cascade,
  direction       public.swipe_direction not null,
  swiped_by       uuid not null references public.profiles (id) on delete cascade,
  -- clock_timestamp so "who swiped first" is ordered even within one transaction
  created_at      timestamptz not null default clock_timestamp(),
  unique (from_listing_id, to_listing_id),
  check (from_listing_id <> to_listing_id)
);
create index swipes_cap_idx on public.swipes (swiped_by, created_at);

-- Pair stored as (least, greatest): the check plus unique is unique(least(a,b), greatest(a,b)).
create table public.matches (
  id             uuid primary key default gen_random_uuid(),
  competition_id uuid not null references public.competitions (id) on delete cascade,
  listing_a      uuid not null references public.listings (id) on delete cascade,
  listing_b      uuid not null references public.listings (id) on delete cascade,
  created_at     timestamptz not null default now(),
  check (listing_a < listing_b),
  unique (listing_a, listing_b)
);

create table public.endorsements (
  id              uuid primary key default gen_random_uuid(),
  from_profile_id uuid not null references public.profiles (id) on delete cascade,
  to_profile_id   uuid not null references public.profiles (id) on delete cascade,
  competition_id  uuid not null references public.competitions (id) on delete cascade,
  positive        boolean not null,
  created_at      timestamptz not null default now(),
  unique (from_profile_id, to_profile_id, competition_id),
  check (from_profile_id <> to_profile_id)
);

create table public.badges (
  code        text primary key,
  name        text not null,
  description text not null
);
insert into public.badges (code, name, description) values
  ('first_team',       'First Team',   'Formed or joined your first team'),
  ('three_hackathons', '3 Hackathons', 'Finished three competitions'),
  ('reliable',         'Reliable',     'Received five positive endorsements'),
  ('finalist',         'Finalist',     'Reached the finals, confirmed by a teammate'),
  ('winner',           'Winner',       'Won a competition, confirmed by a teammate');

create table public.profile_badges (
  profile_id uuid not null references public.profiles (id) on delete cascade,
  badge_code text not null references public.badges (code),
  awarded_at timestamptz not null default now(),
  primary key (profile_id, badge_code)
);

-- XP ledger: the unique key makes every award idempotent; profiles.xp is its running sum.
create table public.xp_events (
  profile_id uuid not null references public.profiles (id) on delete cascade,
  reason     public.xp_reason not null,
  ref_id     uuid not null,
  amount     int not null,
  created_at timestamptz not null default now(),
  primary key (profile_id, reason, ref_id)
);

create table public.team_results (
  team_id      uuid primary key references public.teams (id) on delete cascade,
  result       public.team_result not null,
  reported_by  uuid not null references public.profiles (id) on delete cascade,
  confirmed_by uuid references public.profiles (id) on delete cascade,
  check (confirmed_by <> reported_by)
);
