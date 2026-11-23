-- Hosted dev DB only: fill in details for the two competitions it already has, and add three clearly
-- sample upcoming hackathons so search and filters have something to work on. Safe to re-run.
-- Not a migration, so production never gets sample events. Locally, seed.sql already has all of this.
--   psql "$DB" -v ON_ERROR_STOP=1 -f supabase/snippets/competition_details_dev_data.sql

begin;

update public.competitions set
  organizer = 'Student Tech Society', prize = 'Rp 50.000.000', mode = 'offline', city = 'Yogyakarta',
  tags = '{Mobile,Web,Social impact}'
where id = 'c0000000-0000-0000-0000-000000000001';

update public.competitions set
  organizer = 'Data Science Student Association', prize = 'Rp 25.000.000', mode = 'online', city = null,
  tags = '{Data,AI}'
where id = 'c0000000-0000-0000-0000-000000000002';

insert into public.competitions (id, name, description, url, registration_deadline, event_end, min_team_size, max_team_size,
                                 organizer, prize, mode, city, tags) values
  ('c0000000-0000-0000-0000-000000000003', 'Nusantara AI Build Week',
   'Sample event. One week to ship an AI tool for small businesses, with a demo day in Bandung.',
   'https://example.com/hackathons/nusantara-ai-build-week',
   '2026-12-20 16:59:00+00', '2027-01-10 10:00:00+00', 2, 5,
   'Nusantara Dev Community', 'Rp 75.000.000 + cloud credits', 'hybrid', 'Bandung', '{AI,Data,Web}'),
  ('c0000000-0000-0000-0000-000000000004', 'Green Cities Hack',
   'Sample event. A weekend on transport, waste and air quality in Indonesian cities.',
   'https://example.com/hackathons/green-cities-hack',
   '2027-01-15 16:59:00+00', '2027-02-01 10:00:00+00', 3, 5,
   'Urban Futures Lab', 'Rp 40.000.000', 'offline', 'Surabaya', '{Social impact,Mobile,IoT}'),
  ('c0000000-0000-0000-0000-000000000005', 'Open Ledger Online Jam',
   'Sample event. Build an open-source app on a public blockchain testnet, fully remote.',
   'https://example.com/hackathons/open-ledger-jam',
   '2026-12-05 16:59:00+00', '2026-12-13 10:00:00+00', 1, 4,
   'Open Ledger Guild', '$10,000 + swag', 'online', null, '{Web3,Web}')
on conflict (id) do nothing;

commit;
