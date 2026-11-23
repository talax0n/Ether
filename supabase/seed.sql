-- Local demo data. Sign in as demoN@student.example.ac.id (N = 1..8) with the password ether-demo.
-- Local only: never seed a known password into a shared project.

insert into auth.users (
  instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
  raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
  confirmation_token, recovery_token, email_change_token_new, email_change,
  email_change_token_current, phone_change, phone_change_token, reauthentication_token
)
select '00000000-0000-0000-0000-000000000000',
       ('d0000000-0000-0000-0000-00000000000' || n)::uuid,
       'authenticated', 'authenticated',
       'demo' || n || '@student.example.ac.id', extensions.crypt('ether-demo', extensions.gen_salt('bf')), now(),
       '{"provider":"email","providers":["email"]}', jsonb_build_object('name', 'Demo ' || n),
       now(), now(), '', '', '', '', '', '', '', ''
from generate_series(1, 8) n;

insert into auth.identities (id, provider_id, user_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
select u.id, u.id::text, u.id, jsonb_build_object('sub', u.id::text, 'email', u.email, 'email_verified', true),
       'email', now(), now(), now()
from auth.users u where u.email like 'demo%@student.example.ac.id';

update public.profiles p set
  name = d.name, major = d.major, year = d.year, bio = d.bio, skills = d.skills::text[], roles = d.roles::text[]
from (values
  (1, 'Ayu Lestari',     'Computer Science',       3, 'Backend dev who likes Postgres a bit too much.', '{Kotlin,PostgreSQL,Go}',       '{Backend}'),
  (2, 'Budi Santoso',    'Information Systems',    2, 'Product thinker, ran two campus startups.',     '{Figma,Research,Pitching}',  '{PM,Designer}'),
  (3, 'Citra Dewi',      'Visual Communication',   4, 'UI designer, loves motion and type.',           '{Figma,Illustrator,Motion}', '{Designer}'),
  (4, 'Dimas Pratama',   'Computer Engineering',   3, 'Embedded and IoT tinkerer.',                    '{C,Arduino,Python}',         '{Hardware,Backend}'),
  (5, 'Eka Putri',       'Statistics',             2, 'Data science, Kaggle bronze.',                  '{Python,Pandas,PyTorch}',    '{Data,ML}'),
  (6, 'Fajar Nugroho',   'Computer Science',       1, 'Android dev, Jetpack Compose fan.',             '{Kotlin,Compose,Firebase}',  '{Mobile}'),
  (7, 'Gita Maharani',   'Business Administration',3, 'Pitch deck and business model person.',         '{Pitching,Finance,Canva}',   '{PM,Business}'),
  (8, 'Hendra Wijaya',   'Computer Science',       4, 'Full-stack web, React and Supabase.',           '{TypeScript,React,Supabase}','{Frontend,Backend}')
) d(n, name, major, year, bio, skills, roles)
where p.id = ('d0000000-0000-0000-0000-00000000000' || d.n)::uuid;

-- A mix of contacts: WhatsApp only, Instagram only, LINE only, and combinations.
insert into public.profile_contacts (profile_id, phone, instagram, line_id)
select ('d0000000-0000-0000-0000-00000000000' || n)::uuid,
       case when n in (1, 2, 4, 6, 8) then '62812000000' || n end,
       case when n in (2, 3, 5, 8) then 'demo' || n || '.ether' end,
       case when n in (4, 5, 7) then 'demo' || n || '_line' end
from generate_series(1, 8) n;

insert into public.competitions (id, name, description, url, registration_deadline, event_end, min_team_size, max_team_size,
                                 organizer, prize, mode, city, tags) values
  ('c0000000-0000-0000-0000-000000000001', 'Campus Hackathon 2026',
   '48-hour hackathon on campus. Theme: tools for students.', 'https://example.ac.id/hackathon',
   now() + interval '14 days', now() + interval '21 days', 2, 4,
   'Student Tech Society', 'Rp 50.000.000', 'offline', 'Yogyakarta', '{Mobile,Web,Social impact}'),
  ('c0000000-0000-0000-0000-000000000002', 'Data Challenge 2026',
   'Finished data science challenge, used to test endorsements.', 'https://example.ac.id/data',
   now() - interval '30 days', now() - interval '7 days', 2, 3,
   'Data Science Student Association', 'Rp 25.000.000', 'online', null, '{Data,AI}'),
  ('c0000000-0000-0000-0000-000000000003', 'Nusantara AI Build Week',
   'Sample event. One week to ship an AI tool for small businesses, with a demo day in Bandung.',
   'https://example.com/hackathons/nusantara-ai-build-week',
   now() + interval '26 days', now() + interval '47 days', 2, 5,
   'Nusantara Dev Community', 'Rp 75.000.000 + cloud credits', 'hybrid', 'Bandung', '{AI,Data,Web}'),
  ('c0000000-0000-0000-0000-000000000004', 'Green Cities Hack',
   'Sample event. A weekend on transport, waste and air quality in Indonesian cities.',
   'https://example.com/hackathons/green-cities-hack',
   now() + interval '52 days', now() + interval '69 days', 3, 5,
   'Urban Futures Lab', 'Rp 40.000.000', 'offline', 'Surabaya', '{Social impact,Mobile,IoT}'),
  ('c0000000-0000-0000-0000-000000000005', 'Open Ledger Online Jam',
   'Sample event. Build an open-source app on a public blockchain testnet, fully remote.',
   'https://example.com/hackathons/open-ledger-jam',
   now() + interval '11 days', now() + interval '19 days', 1, 4,
   'Open Ledger Guild', '$10,000 + swag', 'online', null, '{Web3,Web}');

-- Finished competition: one full team, so its members can endorse each other. Team and listing
-- are backdated to before the registration deadline, as if formed during registration.
insert into public.teams (id, competition_id, owner_id, name, pitch, open_roles, created_at) values
  ('a0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002',
   'd0000000-0000-0000-0000-000000000001', 'Night Owls', 'We predict campus bus delays.', '{}',
   now() - interval '40 days');
insert into public.team_members (team_id, profile_id) values
  ('a0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002'),
  ('a0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000003');
insert into public.listings (competition_id, kind, team_id, active, created_at) values
  ('c0000000-0000-0000-0000-000000000002', 'team', 'a0000000-0000-0000-0000-000000000002', false,
   now() - interval '40 days');
select public.finish_competition('c0000000-0000-0000-0000-000000000002');

-- Upcoming competition: six solos and one team with open slots.
insert into public.teams (id, competition_id, owner_id, name, pitch, open_roles) values
  ('a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001',
   'd0000000-0000-0000-0000-000000000007', 'Byte Club', 'Campus marketplace for used textbooks.', '{Mobile,Designer}');
insert into public.team_members (team_id, profile_id) values
  ('a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000008');
insert into public.listings (competition_id, kind, team_id, created_at) values
  ('c0000000-0000-0000-0000-000000000001', 'team', 'a0000000-0000-0000-0000-000000000001', now() - interval '10 minutes');
insert into public.listings (id, competition_id, kind, profile_id, created_at)
select ('b0000000-0000-0000-0000-00000000000' || n)::uuid, 'c0000000-0000-0000-0000-000000000001', 'solo',
       ('d0000000-0000-0000-0000-00000000000' || n)::uuid, now() - (n || ' minutes')::interval
from generate_series(1, 6) n;

-- Track record: a few past hackathons and projects, so deck cards show a summary.
insert into public.profile_hackathons (profile_id, name, year, result, project_name, url)
select ('d0000000-0000-0000-0000-00000000000' || n)::uuid, name, year, result::public.hackathon_result, project_name, url
from (values
  (1, 'GEMASTIK',               2025, 'winner',            'BusKu',        'https://github.com/example/busku'),
  (1, 'Campus Hackathon',       2024, 'participant',       null,           null),
  (2, 'Hackathon Jakarta',      2025, 'finalist',          'Warung Kas',   null),
  (3, 'UI/UX Sprint',           2024, 'honorable_mention', 'Kampus Map',   'https://dribbble.com/example'),
  (5, 'Data Science Weekend',   2025, 'finalist',          null,           null),
  (8, 'GEMASTIK',               2024, 'participant',       'Sewa Buku',    null),
  (8, 'Web Dev Cup',            2025, 'winner',            'Kos Finder',   'https://kosfinder.example.com')
) d(n, name, year, result, project_name, url);

insert into public.profile_projects (profile_id, title, blurb, url, tags)
select ('d0000000-0000-0000-0000-00000000000' || n)::uuid, title, blurb, url, tags::text[]
from (values
  (1, 'BusKu',        'Predicts campus bus arrivals from GPS pings.',          'https://github.com/example/busku', '{Kotlin,PostgreSQL}'),
  (1, 'pg-lint',      'A tiny linter for Supabase migrations.',                null,                               '{Go,PostgreSQL}'),
  (3, 'Kampus Map',   'Indoor map for the faculty building, with room search.', 'https://dribbble.com/example',     '{Figma,Motion}'),
  (6, 'Absensi',      'Compose attendance app with QR check-in.',              null,                               '{Kotlin,Compose,Firebase}'),
  (8, 'Kos Finder',   'Search boarding houses near campus by price and walk time.', 'https://kosfinder.example.com', '{React,Supabase}')
) d(n, title, blurb, url, tags);
