# Ether

Android app for finding hackathon teammates at one university by swiping. Product scope lives in [SPEC.md](SPEC.md). Build order lives in [PLAN.md](PLAN.md). Read SPEC §3 (domain model) before touching data code.

## Stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Koin · Coil · supabase-kt. Single `app` module. Supabase migrations in `supabase/migrations/`.

## Rules

- Business rules (matching, swipe cap, XP, contact visibility) live in Postgres: constraints, triggers, RLS. Never re-implement them in the client as the source of truth.
- Parse Supabase rows into domain types in the `data` layer. UI and ViewModels only see domain types.
- Model closed sets as sealed types or enums (`ListingKind`, `SwipeDirection`). No stringly-typed state.
- Screens expose one `StateFlow<UiState>` from the ViewModel. Composables stay stateless where practical.
- No secrets in source. The service role key never goes in the app.
- Anything in SPEC §7 (Non-goals) needs a SPEC change first, not a quiet addition.
- Verify UI changes on an emulator or device, not just by compiling.
