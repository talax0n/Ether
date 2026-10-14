# Decision log (overnight autonomous run, 2026-10-15)

Operator granted full autonomy and went to sleep. Each entry: decision, why, and how to reverse it.

## Done predicate ("demo ready")

The run is finished only when all of these hold on the emulator against local Supabase (real backend, not fakes):

- [ ] `supabase db reset` + `supabase test db` green
- [ ] `./gradlew assembleDebug testDebugUnitTest` green
- [ ] Maestro flow: sign in with email OTP (code read from Mailpit) → profile setup → pick competition → join solo → swipe deck
- [ ] Maestro flow: two seeded accounts swipe right on each other → both see the match with the contact link → team formed
- [ ] Create team → team listing appears in another user's deck
- [ ] Endorse a teammate on the ended competition → XP, badge, and reliability score visible on the profile
- [ ] Empty, loading, and error states render (no blank screens or crashes)
- [ ] Screenshots and a screen recording of the full flow saved to `docs/demo/`
- [ ] One-command demo start script (`scripts/demo.sh`): starts Supabase, boots the emulator, installs the app

1. **Placeholder email domain `student.example.ac.id`.** The real university domain was not provided. To change it, update the `allowed_email_domain` row in `app_config`.
2. **Local Supabase only (Docker).** No cloud credentials were provided. Moving to cloud means creating a project, running `supabase link` + `supabase db push`, and setting `SUPABASE_URL`/`SUPABASE_ANON_KEY`.
3. **Supabase CLI from the GitHub release binary (`~/.local/bin/supabase`).** Homebrew formula installs fail because Xcode 26.2 is older than brew requires.
4. **Android SDK via Homebrew cask `android-commandlinetools` + `sdkmanager`, target API 36, arm64 emulator image.**
5. **Two parallel builders in separate worktrees.** `feat/db-schema` (database) and `feat/android-skeleton` (app) share a contract fixed in their briefs. Separate checkouts avoid shared-file conflicts.
6. **Cross-provider review.** Each merged branch gets an independent review from a GPT model (Codex provider) before it lands on `main`.
7. **DB track landed on `feat/db-schema` (c671872).** Rerun by the coordinator: `supabase test db` 50/50 pass. The domain check uses an `auth.users` trigger, so a rejected sign-up surfaces as a generic 500 and the app must show the "use your university email" message itself.
8. **Swiping after `registration_deadline` will be blocked** (DB agent left it open). Cheap, and stops deck noise for closed events. **The deck stays newest-first with no open-role filter**, which is enough for the MVP.
9. **"Competition finished" XP via the admin `finish_competition(id)`**, not a scheduler. For the demo the seed already contains an ended competition.
10. **GPT-6-Sol review of the DB found 5 issues, all accepted** (3 proven with SQL). One solo could join two teams on simultaneous matches. Teams could form after an event and claim winner XP (proven: 50→375 XP). Changing email bypassed the domain rule. Result confirmation could race. A member could see their own team in the deck. A fix round adds a failing test before each fix.
11. **DB fix round merged to `main` (e3580cc).** The coordinator reran it: 67/67 pgTAP pass. A unique `(profile_id, competition_id)` on `team_members` prevents double-team joins. Registration closes at `registration_deadline`. The email-domain check now runs on update. New error codes the app must map: `registration_closed`, `already_in_team`, `listing_inactive`, `team_formed_after_deadline`, `result_already_confirmed`, `email_domain_not_allowed`.
12. **Android SDK complete** after a retry loop. The emulator download hit "Connection reset" once.
13. **Android skeleton merged to `main` (1aef808).** The coordinator reran it: assembleDebug plus 9 unit tests green, and checked the emulator screenshots (drag tilt, LIKE stamp, match dialog). compileSdk is 37 because current AndroidX requires it; targetSdk stays 36.
14. **Track C (`feat/integration`, Claude Opus 5.5)** makes the app run against the real local Supabase. It adds error mapping, an atomic `create_team` RPC, the My Teams/endorsement screen, and loading/empty/error states, with screenshots in `docs/demo/`.
15. **The Android code goes to a GPT-6-Sol review on the second OpenAI account (`codex_spektrum`)**, which spreads usage across both accounts. Its findings feed the next fix round after Track C lands.
16. **The GPT-6-Sol Android review found 8 issues.** The coordinator confirmed #1 by reading the SQL: profile save uses `upsert` but `authenticated` has only an UPDATE grant. The other findings: new users skip onboarding; a team owner gets stuck on an inactive solo listing; double-swipe during a pending RPC; a button racing a drag; refill errors shown as an empty deck; a nullable `description` decoded as non-null; no contract tests. **The fix round is held until Track C lands**, so two writers don't touch the same files.
17. **Track C merged (556efd8).** The coordinator reran it: 79 pgTAP + 26 unit tests green, and checked the match and profile screenshots. 10 real-backend bugs were fixed, including review findings #1, #2, #3 and #7. **Teammates can always see each other's contact links**, which the Teams screen needs. **Result options are finalist/winner only**, matching the DB enum. "Participated" is covered by the competition-finished XP.
18. **Final round (Track D)** covers review findings #4 (double swipe), #5 (button vs drag), #6 (refill error shown as empty), and #8 (contract tests). It also fixes the BACK-in-create-team-dialog exit, and adds Maestro e2e flows, a screen recording, and `scripts/demo.sh`.
