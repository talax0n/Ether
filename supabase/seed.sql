-- Local demo data. Log in as demoN@student.example.ac.id (N = 1..8) with email OTP;
-- the code arrives in Mailpit at http://127.0.0.1:54324.

insert into auth.users (
  instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
  raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
  confirmation_token, recovery_token, email_change_token_new, email_change,
  email_change_token_current, phone_change, phone_change_token, reauthentication_token
)
select '00000000-0000-0000-0000-000000000000',
       ('d0000000-0000-0000-0000-00000000000' || n)::uuid,
       'authenticated', 'authenticated',
       'demo' || n || '@student.example.ac.id', '', now(),
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

insert into public.profile_contacts (profile_id, contact_url)
select ('d0000000-0000-0000-0000-00000000000' || n)::uuid, 'https://wa.me/62812000000' || n
from generate_series(1, 8) n;

insert into public.competitions (id, name, description, url, registration_deadline, event_end, min_team_size, max_team_size) values
  ('c0000000-0000-0000-0000-000000000001', 'Campus Hackathon 2026',
   '48-hour hackathon on campus. Theme: tools for students.', 'https://example.ac.id/hackathon',
   now() + interval '14 days', now() + interval '21 days', 2, 4),
  ('c0000000-0000-0000-0000-000000000002', 'Data Challenge 2026',
   'Finished data science challenge, used to test endorsements.', 'https://example.ac.id/data',
   now() - interval '30 days', now() - interval '7 days', 2, 3);

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
