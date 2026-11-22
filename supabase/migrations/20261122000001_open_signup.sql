-- Sign-up is open to any email and to Google accounts (SPEC §5). The domain guard goes away.
drop trigger email_domain_guard on auth.users;
drop function public.check_email_domain();
drop function public.email_domain_allowed(text);
delete from public.app_config where key = 'allowed_email_domain';

-- Google fills user_metadata with full_name/name and avatar_url/picture; email sign-ups have neither.
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into profiles (id, name, avatar_url)
  values (
    new.id,
    coalesce(
      nullif(new.raw_user_meta_data ->> 'name', ''),
      nullif(new.raw_user_meta_data ->> 'full_name', ''),
      split_part(new.email, '@', 1)
    ),
    coalesce(nullif(new.raw_user_meta_data ->> 'avatar_url', ''), nullif(new.raw_user_meta_data ->> 'picture', ''))
  );
  return new;
end $$;
